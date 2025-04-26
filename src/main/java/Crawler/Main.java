package Crawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting web crawler, how many crawlers do you want to run?");
        System.out.println("Please enter a number between 1 and 20");
        Scanner scanner = new Scanner(System.in);
        int numCrawlers = scanner.nextInt();
        scanner.close();
        List<String> seedURLs = new ArrayList<>();
        Queue<String> urlFrontier = new LinkedList<>();
        Set<String> visitedURLs = new HashSet<>();
        Set<String> visitedDocs = new HashSet<>();
        Set<String> crawledURLs = new HashSet<>();
        AtomicInteger pagesCrawled = new AtomicInteger(0);
        Map<String, List<Pattern>> unAllowedURLs = new HashMap<>();
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");

        // Access the database (it will create it if it doesn't exist)
        MongoDatabase database = mongoClient.getDatabase("searchengine");

        // Access the collection (it will create it if it doesn't exist)
        MongoCollection<org.bson.Document> docsCollection = database.getCollection("Documents");
        MongoCollection<org.bson.Document> URLsCollection = database.getCollection("visitedURLs");
        MongoCollection<org.bson.Document> visitedDocsCollection =
                database.getCollection("hashedDocs");
        MongoCollection<org.bson.Document> crawledURLsCollection =
                database.getCollection("crawledURLs");

        for (org.bson.Document url : URLsCollection.find()) {
            String urlString = url.getString("url");
            visitedURLs.add(urlString);
            urlFrontier.add(urlString);
        }
        for (org.bson.Document url : crawledURLsCollection.find()) {
            String urlString = url.getString("url");
            crawledURLs.add(urlString);
        }
        for (org.bson.Document url : visitedDocsCollection.find()) {
            String doc = url.getString("doc");
            visitedDocs.add(doc);
        }



        seedURLs.add("https://www.bbc.com");
        seedURLs.add("https://www.cnn.com");
        seedURLs.add("https://www.reuters.com");

        // Educational Sites
        seedURLs.add("https://www.wikipedia.org");
        seedURLs.add("https://www.mit.edu");
        seedURLs.add("https://www.stanford.edu");

        // Technology Sites
        seedURLs.add("https://www.github.com");
        seedURLs.add("https://www.stackoverflow.com");
        seedURLs.add("https://developer.mozilla.org");

        // E-commerce Sites
        seedURLs.add("https://www.amazon.com");
        seedURLs.add("https://www.etsy.com");

        // Government Sites
        seedURLs.add("https://www.nasa.gov");
        seedURLs.add("https://www.usa.gov");

        // General Purpose Sites
        seedURLs.add("https://www.reddit.com");
        seedURLs.add("https://www.medium.com");


        try {
            long startTime = System.currentTimeMillis();
            List<Thread> threads = new ArrayList<>(10);
            for (int i = 0; i < numCrawlers; i++) {
                Crawler crawler = new Crawler(seedURLs, i, visitedURLs, pagesCrawled, urlFrontier,
                        unAllowedURLs, visitedDocs, crawledURLs, docsCollection, URLsCollection,
                        visitedDocsCollection, crawledURLsCollection);
                threads.add(new Thread(crawler));
                threads.get(i).start();
            }
            for (int i = 0; i < numCrawlers; i++) {
                threads.get(i).join();
                System.out.println("Thread " + i + " finished");
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Crawlers finished in " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mongoClient.close();
    }
}
