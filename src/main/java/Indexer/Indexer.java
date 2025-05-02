package Indexer;

import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import db.DBManager;
import org.bson.Document;
import Indexer.Tokenizer.Token;

public class Indexer {

   private static DBManager db = new DBManager();
   // Dynamic batch size calculation based on available processors
   private static final int BATCH_SIZE = Runtime.getRuntime().availableProcessors() * 30;
   private static PrintWriter statsLogger;
   private static long totalTokens = 0;
   private static long totalFetchTime = 0;
   private static long totalTokenizeTime = 0;
   private static long totalDbInsertTime = 0;
   
   static {
       try {
           // Create logs directory if it doesn't exist
           File logsDir = new File("logs");
           if (!logsDir.exists()) {
               logsDir.mkdirs();
           }
           
           // Initialize simple stats logger
           DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
           String timestamp = dtf.format(LocalDateTime.now());
           statsLogger = new PrintWriter(new FileWriter("logs/indexing_stats_" + timestamp + ".log"));
           
           // Write headers
           statsLogger.println("=== INDEXING STATISTICS ===");
           statsLogger.println("Started: " + timestamp);
           statsLogger.println("Batch size: " + BATCH_SIZE);
           statsLogger.println("=========================");
           statsLogger.flush();
       } catch (IOException e) {
           System.err.println("\u001B[31m[ERROR] Failed to initialize logger: " + e.getMessage() + "\u001B[0m");
       }
   }
   
   private static synchronized void logPerformanceStats(int documentsIndexed, int batchCount) {
       if (statsLogger != null) {
           statsLogger.println("\n=== PERFORMANCE METRICS ===");
           statsLogger.println("Total documents indexed: " + documentsIndexed);
           statsLogger.println("Total tokens processed: " + totalTokens);
           statsLogger.println("Number of batches: " + batchCount);
           statsLogger.println("Average tokens per document: " + (documentsIndexed > 0 ? totalTokens / documentsIndexed : 0));
           statsLogger.println("\n=== TIMING BREAKDOWN ===");
           statsLogger.println("Total document fetch time: " + totalFetchTime + "ms");
           statsLogger.println("Total tokenization time: " + totalTokenizeTime + "ms");
           statsLogger.println("Total database insertion time: " + totalDbInsertTime + "ms");
           statsLogger.println("Average fetch time per batch: " + (batchCount > 0 ? totalFetchTime / batchCount : 0) + "ms");
           statsLogger.println("Average tokenization time per document: " + 
               (documentsIndexed > 0 ? totalTokenizeTime / documentsIndexed : 0) + "ms");
           statsLogger.println("Average database insertion time per document: " + 
               (documentsIndexed > 0 ? totalDbInsertTime / documentsIndexed : 0) + "ms");
           
           statsLogger.println("\n=== PERFORMANCE RECOMMENDATIONS ===");
           // Determine performance bottlenecks
           if (totalFetchTime > totalTokenizeTime && totalFetchTime > totalDbInsertTime) {
               statsLogger.println("1. Database retrieval appears to be the bottleneck");
               statsLogger.println("   - Consider adding indexes to the 'isIndexed' field");
               statsLogger.println("   - Increase batch size to reduce number of queries");
               statsLogger.println("   - Check network connection to MongoDB if using a remote server");
           } else if (totalDbInsertTime > totalTokenizeTime && totalDbInsertTime > totalFetchTime) {
               statsLogger.println("1. Database insertion appears to be the bottleneck");
               statsLogger.println("   - Consider increasing the bulk write size");
               statsLogger.println("   - Check if your MongoDB indexes are optimized");
               statsLogger.println("   - Consider using SSD storage for MongoDB");
           } else if (totalTokenizeTime > totalFetchTime && totalTokenizeTime > totalDbInsertTime) {
               statsLogger.println("1. Tokenization appears to be the bottleneck");
               statsLogger.println("   - Consider optimizing the tokenization algorithm");
               statsLogger.println("   - Evaluate if you can reduce the complexity of token processing");
           }
           
           statsLogger.println("2. General performance tips:");
           statsLogger.println("   - Increase JVM heap size if possible");
           statsLogger.println("   - Optimize MongoDB connection settings");
           statsLogger.println("   - Consider using a more powerful machine for indexing");
           
           statsLogger.flush();
       }
   }
   
   public static void index() {
       long startTime = System.currentTimeMillis(); 
       System.out.println("\u001B[34m[INFO] Starting the indexing process with batch size: " + BATCH_SIZE + "...\u001B[0m");

       try {
           // Optimize thread count for indexing
           int numThreads = Runtime.getRuntime().availableProcessors();
           // Use 2 threads per core, capped at 24 for manageability
           numThreads = Math.min(numThreads * 2, 24);
           System.out.println("[INFO] Using " + numThreads + " threads for indexing");
           ExecutorService executor = Executors.newFixedThreadPool(numThreads);

           Tokenizer[] tokenizers = new Tokenizer[numThreads];
           for (int i = 0; i < numThreads; i++) {
               tokenizers[i] = new Tokenizer();
           }

           int documentsIndexed = 0;
           int batchCount = 0;
           Queue<Document> documentQueue = new ConcurrentLinkedQueue<>();

           while (true) {
               long batchStartTime = System.currentTimeMillis();
               List<Document> unindexedDocs = db.getUnIndexedDocs(BATCH_SIZE);
               long fetchTime = System.currentTimeMillis() - batchStartTime;
               totalFetchTime += fetchTime;
               
               System.out.println("[INFO] Fetched " + unindexedDocs.size() + " documents for batch " + batchCount);
               
               if (unindexedDocs.isEmpty()) {
                   System.out.println("[INFO] No more unindexed documents found. Exiting...");
                   break;
               }

               documentQueue.addAll(unindexedDocs);
               documentsIndexed += unindexedDocs.size();
               
               CountDownLatch latch = new CountDownLatch(unindexedDocs.size());
               AtomicInteger threadAssignment = new AtomicInteger(0);
               
               for (int i = 0; i < unindexedDocs.size(); i++) {
                   Document doc = documentQueue.poll();
                   if (doc == null) break;
                   
                   final int threadId = threadAssignment.getAndIncrement() % numThreads;
                   executor.submit(() -> {
                       try {
                           processDocument(doc, tokenizers[threadId]);
                       } finally {
                           latch.countDown();
                       }
                   });
               }

               if (!latch.await(30, TimeUnit.MINUTES)) {
                   System.err.println("\u001B[31m[ERROR] Timed out waiting for batch to complete\u001B[0m");
               }
               
               long batchEndTime = System.currentTimeMillis();
               long batchDuration = batchEndTime - batchStartTime;
               
               String batchCompleteMsg = "[BATCH " + batchCount + "] Completed in " + batchDuration + "ms";
               System.out.println("\u001B[34m" + batchCompleteMsg + "\u001B[0m");
               
               batchCount++;
           }

           String completionMsg = "[INFO] Indexed " + documentsIndexed + " documents in " + batchCount + " batches";
           System.out.println("\u001B[36m" + completionMsg + "\u001B[0m");
           
           logPerformanceStats(documentsIndexed, batchCount);

           executor.shutdown();
           if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
               System.err.println("\u001B[31m[ERROR] Executor did not terminate in the specified time\u001B[0m");
               executor.shutdownNow();
           }
       } catch (InterruptedException e) {
           System.err.println("\u001B[31m[ERROR] Thread pool interrupted: " + e.getMessage() + "\u001B[0m");
           Thread.currentThread().interrupt();
       } finally {
           db.close();
           System.out.println("[INFO] MongoClient closed.");
           
           if (statsLogger != null) {
               statsLogger.close();
           }
       }

       long endTime = System.currentTimeMillis(); 
       long totalDuration = endTime - startTime;
       
       String completionMsg = "[INFO] Indexing process completed in " + totalDuration + " ms";
       System.out.println("\u001B[34m" + completionMsg + "\u001B[0m");
       
       try (PrintWriter finalLogger = new PrintWriter(new FileWriter("logs/indexing_summary.log", true))) {
           DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
           String timestamp = dtf.format(LocalDateTime.now());
           finalLogger.println(timestamp + ",TotalRunTime," + totalDuration + ",TotalTokens," + totalTokens);
       } catch (IOException e) {
           System.err.println("\u001B[31m[ERROR] Failed to write final timing log: " + e.getMessage() + "\u001B[0m");
       }
   }
   
   private static void processDocument(Document doc, Tokenizer tokenizer) {
       String title = doc.getString("title");
       try {
           String body = doc.getString("body");
           if (body == null || body.isBlank()) {
               System.out.println("\u001B[33m[WARNING] Document '" + title + "' has null or empty body. Marking as indexed.\u001B[0m");
               db.markDocumentAsIndexed(doc.getObjectId("_id").toString());
               return;
           }
           
           long tokenizeStartTime = System.currentTimeMillis();
           HashMap<String, Token> tokens = tokenizer.tokenizeDoc(doc);
           long tokenizeTime = System.currentTimeMillis() - tokenizeStartTime;
           
           synchronized(Indexer.class) {
               totalTokenizeTime += tokenizeTime;
               totalTokens += tokens.size();
           }
           
           if (!tokens.isEmpty()) {
               long dbStartTime = System.currentTimeMillis();
               db.insertInverted(doc.getObjectId("_id").toString(), tokens);
               long dbTime = System.currentTimeMillis() - dbStartTime;
               
               synchronized(Indexer.class) {
                   totalDbInsertTime += dbTime;
               }
           } else {
               db.markDocumentAsIndexed(doc.getObjectId("_id").toString());
           }
       } catch (Exception e) {
           System.err.println("\u001B[31m[ERROR] Error indexing document '" + title + "': " + e.getMessage() + "\u001B[0m");
       }
   }
}
