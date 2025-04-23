package main.java.indexer;

import indexer.Tokenizer;
import indexer.Tokenizer.Token;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.text.Document;

public class Indexer {

    private static DBManager db = new DBManager();

    public static void index() {
        List<Document> unindexedDocs = db.getUnIndexedDocs(100);
        Tokenizer tokenizer = new Tokenizer();
        int patchNumber = 0;
        int numThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThread);

        while(unindexedDocs.size() > 0) {

            System.out.println("=================================");
            System.err.println("start indexing patch number: " + patchNumber);
            System.out.println("=================================");


            for (Document doc : unindexedDocs) {
                executor.submit(() -> {
                    String title = doc.getString("title");
                    String content = doc.getString("content");
                    org.jsoup.nodes.Document contentDoc = Jsoup.parse(content);
                    System.out.println("=>Indexing: " + title);
                    HashMap<String, Token> tokens = tokenizer.tokenizeDoc(contentDoc);
                    System.out.println("=>Tokens: " + tokens.size() + " in " + title);
                    try {
                        db.insertInverted(doc.getObjectId("_id").toString(), tokens);
                    } catch (Exception e) {
                        e.getMessage();
                    }
                    System.out.println("=>Inserted into db: " + title);

                });
            }

            executor.shutdown();
            while (!executor.isTerminated()) {}
            unindexedDocs = db.getUnIndexedDocs(100);
            patchNumber++;
            executor = Executors.newFixedThreadPool(numThread);
        }

        // Calculate popularity here
    }
}
