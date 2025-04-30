package indexer;

import indexer.Tokenizer.Token;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import db.DBManager;
import org.bson.Document;

public class Indexer {

   private static DBManager db = new DBManager();

   public static void index() {
       long startTime = System.currentTimeMillis(); 
       System.out.println("\u001B[34m[INFO] Starting the indexing process...\u001B[0m");

       int patchNumber = 0;

       int numThreads = Runtime.getRuntime().availableProcessors();
       ExecutorService executor = Executors.newFixedThreadPool(numThreads);

       while (true) {
           List<Document> unindexedDocs = db.getUnIndexedDocs(50);
           if (unindexedDocs.isEmpty()) {
               break; // Exit loop if no unindexed documents are left
           }

           System.out.println("\u001B[36m[INFO] Processing patch number: " + patchNumber + "\u001B[0m");
           System.out.println("\u001B[36m[INFO] Number of documents in this patch: " + unindexedDocs.size() + "\u001B[0m");

           for (Document doc : unindexedDocs) {
               executor.submit(() -> {
                   String title = doc.getString("title");
                   System.out.println("\u001B[32m[PROCESS] Indexing document: " + title + "\u001B[0m");
                   try {
                       // Tokenize the document in parallel
                       HashMap<String, Token> tokens = new Tokenizer().tokenizeDoc(doc);
                       System.out.println("\u001B[32m[PROCESS] Tokens generated: " + tokens.size() + " for " + title + "\u001B[0m");
                       db.insertInverted(doc.getObjectId("_id").toString(), tokens);
                       System.out.println("\u001B[32m[PROCESS] Successfully inserted into the database: " + title + "\u001B[0m");
                   } catch (Exception e) {
                       System.err.println("\u001B[31m[ERROR] Error indexing document: " + e.getMessage() + "\u001B[0m");
                   }
               });
           }

           // Wait for all tasks in the current batch to complete
           executor.shutdown();
           try {
               executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.MILLISECONDS);
           } catch (InterruptedException e) {
               System.err.println("\u001B[31m[ERROR] Thread pool interrupted: " + e.getMessage() + "\u001B[0m");
           }

           patchNumber++;
           executor = Executors.newFixedThreadPool(numThreads); // Reinitialize the executor for the next batch
       }

       executor.shutdown();
       long endTime = System.currentTimeMillis(); 
       System.out.println("\u001B[34m[INFO] Indexing process completed.\u001B[0m");
       System.out.println("\u001B[34m[INFO] Total time taken: " + (endTime - startTime) + " ms\u001B[0m");
   }
}
