package src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jsoup.nodes.Document;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting web crawler");
        int numCrawlers = 10;
        List<String> seedURLs = new ArrayList<>();
        Queue<String> urlFrontier = new ConcurrentLinkedQueue<>();
        List<Document> results = Collections.synchronizedList(new ArrayList<>());

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

        Set<String> visitedURLs = Collections.synchronizedSet(new HashSet<>());

        try {
            long startTime = System.currentTimeMillis();
            List<Thread> threads = new ArrayList<>(10);
            for (int i = 0; i < numCrawlers; i++) {
                Crawler crawler = new Crawler(seedURLs, i, visitedURLs, results, urlFrontier);
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
    }
}
