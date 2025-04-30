package Indexer;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import db.DBManager;

import org.bson.Document;

import Indexer.Tokenizer.Token;

public class Indexer {

   private static DBManager db = new DBManager();

   public static void index() {
       long startTime = System.currentTimeMillis(); 
       System.out.println("\u001B[34m[INFO] Starting the indexing process...\u001B[0m");

       try {
           int numThreads = Runtime.getRuntime().availableProcessors();
           ExecutorService executor = Executors.newFixedThreadPool(numThreads);

           int maxDocumentsToIndex = 10; // Limit to 10 documents
           int documentsIndexed = 0;

           while (documentsIndexed < maxDocumentsToIndex) {
               List<Document> unindexedDocs = db.getUnIndexedDocs(10);
               System.out.println("[DEBUG] the total unindexed Docs is " + unindexedDocs.size());
               if (unindexedDocs.isEmpty()) {
                   System.out.println("[INFO] No unindexed documents found. Exiting...");
                   break;
               }

               for (Document doc : unindexedDocs) {
                   if (documentsIndexed >= maxDocumentsToIndex) {
                       break;
                   }

                   executor.submit(() -> {
                       String title = doc.getString("title");
                       System.out.println("\u001B[32m[PROCESS] Indexing document: " + title + "\u001B[0m");
                       try {
                           HashMap<String, Token> tokens = new Tokenizer().tokenizeDoc(doc);
                           System.out.println("[DEBUG] Tokens generated for document: " + title + " -> " + tokens.size());
                           if (tokens.isEmpty()) {
                               System.out.println("[DEBUG] No tokens generated for document: " + title);
                           } else {
                               db.insertInverted(doc.getObjectId("_id").toString(), tokens);
                               System.out.println("\u001B[32m[PROCESS] Successfully inserted into the database: " + title + "\u001B[0m");
                           }
                       } catch (Exception e) {
                           System.err.println("\u001B[31m[ERROR] Error indexing document: " + title + " - " + e.getMessage() + "\u001B[0m");
                       }
                   });

                   documentsIndexed++;
               }

               // Wait for all tasks in the current batch to complete
               executor.shutdown();
               if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                   System.err.println("\u001B[31m[ERROR] Executor did not terminate in the specified time.\u001B[0m");
                   executor.shutdownNow();
               }
           }

           System.out.println("\u001B[36m[INFO] Indexed " + documentsIndexed + " documents. Exiting...\u001B[0m");
       } catch (InterruptedException e) {
           System.err.println("\u001B[31m[ERROR] Thread pool interrupted: " + e.getMessage() + "\u001B[0m");
           Thread.currentThread().interrupt(); // Restore interrupted status
       } finally {
           db.close(); // Ensure MongoClient is closed
           System.out.println("[INFO] MongoClient closed.");
       }

       long endTime = System.currentTimeMillis(); 
       System.out.println("\u001B[34m[INFO] Indexing process completed.\u001B[0m");
       System.out.println("\u001B[34m[INFO] Total time taken: " + (endTime - startTime) + " ms\u001B[0m");
   }
}
