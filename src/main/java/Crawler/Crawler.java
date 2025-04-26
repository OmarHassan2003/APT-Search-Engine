package Crawler;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.mongodb.client.MongoCollection;



public class Crawler implements Runnable {

    private final List<String> seedURLs;
    private final Queue<String> urlFrontier;
    private int ID = 0;

    private final Set<String> visitedURLs;
    private final Set<String> visitedDocs;
    private final Set<String> crawledURLs;
    private final Map<String, List<Pattern>> unAllowedURLs;
    private final MongoCollection<org.bson.Document> docsCollection;
    private final MongoCollection<org.bson.Document> URLsCollection;
    private final MongoCollection<org.bson.Document> visitedDocsCollection;
    private final MongoCollection<org.bson.Document> crawledURLsCollection;
    private static final int MAX_PAGES = 109;
    private final AtomicInteger pagesCrawled;

    public Crawler(List<String> urls, int id, Set<String> visitedURLs, AtomicInteger pagesCrawled,
            Queue<String> urlFrontier, Map<String, List<Pattern>> unAllowedURLs,
            Set<String> visitedDocs, Set<String> crawledURLs,
            MongoCollection<org.bson.Document> docsCollection,
            MongoCollection<org.bson.Document> URLsCollection,
            MongoCollection<org.bson.Document> visitedDocsCollection,
            MongoCollection<org.bson.Document> crawledURLsCollection) {
        this.pagesCrawled = pagesCrawled;
        this.crawledURLs = crawledURLs;
        this.crawledURLsCollection = crawledURLsCollection;
        this.visitedDocsCollection = visitedDocsCollection;
        this.URLsCollection = URLsCollection;
        this.docsCollection = docsCollection;
        this.visitedDocs = visitedDocs;
        this.unAllowedURLs = unAllowedURLs;
        this.seedURLs = urls;
        this.urlFrontier = urlFrontier;
        this.ID = id;
        this.visitedURLs = visitedURLs;
    }

    @Override
    public void run() {
        for (String url : seedURLs) {
            try {
                if (url.contains("{{") || url.contains("}}")) {
                    continue; // skip template URLs
                }
                url = URLNormalizer.normalizeToCompactString(url);
                synchronized (visitedURLs) {
                    if (visitedURLs.contains(url))
                        continue;
                    visitedURLs.add(url);
                }
                synchronized (urlFrontier) {
                    urlFrontier.add(url);
                }
                URLsCollection.insertOne(new org.bson.Document("url", url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        crawl();
    }

    private void crawl() {
        while (pagesCrawled.get() < MAX_PAGES) {
            String url;
            synchronized (urlFrontier) {
                url = urlFrontier.poll();
            }
            if (url == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
                continue;
            }
            if (crawledURLs.contains(url)) {
                continue;
            }
            String base = null;
            try {
                URL baseURL = URI.create(url).toURL();
                base = baseURL.getProtocol() + "://" + baseURL.getHost();
                synchronized (unAllowedURLs) {
                    if (!unAllowedURLs.containsKey(base)) {
                        unAllowedURLs.put(base, null); // Insert null initially to avoid
                                                       // re-parsing when exception occurs
                        List<Pattern> disallowed = RobotParser.parse(base);
                        unAllowedURLs.put(base, disallowed); // Replace null with actual list
                    }
                }
            } catch (Exception e) {
                System.out.println("Error parsing URL: " + e.getMessage());
            }
            if (base != null && unAllowedURLs.containsKey(base)) {
                List<Pattern> disallowed = unAllowedURLs.get(base);
                if (disallowed != null) {
                    if (RobotParser.isDisallowed(url, disallowed)) {
                        continue;
                    }
                }
            }
            org.jsoup.nodes.Document doc = downloadPage(url);
            if (doc == null) {
                continue;
            }
            try {
                String hashedDoc = sha256Hash(doc); // in case two urls point to the same page
                synchronized (visitedDocs) {
                    if (visitedDocs.contains(hashedDoc)) {
                        continue;
                    }
                    visitedDocs.add(hashedDoc);
                }
                visitedDocsCollection
                        .insertOne(new org.bson.Document("url", url).append("doc", hashedDoc));
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Error hashing document: " + e.getMessage());
            }
            ArrayList<String> h1s = new ArrayList<>();
            for (Element h1 : doc.select("h1")) {
                h1s.add(h1.text());
            }
            ArrayList<String> h2s = new ArrayList<>();
            for (Element h2 : doc.select("h2")) {
                h2s.add(h2.text());
            }
            ArrayList<String> h3s = new ArrayList<>();
            for (Element h3 : doc.select("h3")) {
                h3s.add(h3.text());
            }
            ArrayList<String> h456s = new ArrayList<>();
            for (Element h456 : doc.select("h4, h5, h6")) {
                h456s.add(h456.text());
            }
            if (pagesCrawled.get() >= MAX_PAGES) {
                break;
            } else {
                pagesCrawled.incrementAndGet();
            }
            docsCollection.insertOne(new org.bson.Document("title", doc.title()).append("url", url)
                    .append("h1s", h1s).append("h2s", h2s).append("h3s", h3s).append("h456s", h456s)
                    .append("body", doc.body().text()).append("isIndexed", false));
            crawledURLsCollection.insertOne(new org.bson.Document("url", url));
            List<String> links = extractLinks(doc);
            for (String link : links) {
                try {
                    if (link.contains("{{") || link.contains("}}")) {
                        continue; // skip template URLs
                    }
                    link = URLNormalizer.normalizeToCompactString(link);
                    if (link == null) {
                        continue;
                    }
                    if (!visitedURLs.contains(link)) {
                        synchronized (visitedURLs) { // add to visited when adding to queue, not
                                                     // when poping from queue to ensure no
                                                     // duplicates
                            visitedURLs.add(link);
                        }
                        synchronized (urlFrontier) {
                            urlFrontier.add(link);
                        }
                        URLsCollection.insertOne(new org.bson.Document("url", link));
                    }
                } catch (URISyntaxException e) {
                    System.out.println("Error normalizing URL: " + e.getMessage());
                }
            }
        }
        System.out.println(pagesCrawled + " pages" + " crawled by thread " + ID);
    }

    private List<String> extractLinks(org.jsoup.nodes.Document doc) {
        List<String> links = new ArrayList<>();
        for (Element link : doc.select("a[href]")) {
            String absURL = link.attr("abs:href");

            if (absURL != null) {
                links.add(absURL);
            }
        }
        return links;
    }

    private org.jsoup.nodes.Document downloadPage(String url) {
        try {
            Connection connection =
                    Jsoup.connect(url).userAgent("Mozilla/5.0 (compatible; MiniCrawler/1.0)")
                            .timeout(10000).followRedirects(true);

            org.jsoup.nodes.Document doc = connection.get();
            return doc;
        } catch (org.jsoup.HttpStatusException e) {
            // Log HTTP errors (like 404) but don't print stack trace
            System.out.println("HTTP error " + e.getStatusCode() + " for URL: " + url);
            return null;
        } catch (Exception e) {
            // Handle other exceptions
            System.out.println("Error downloading " + url + ": " + e.getMessage());
            return null;
        }
    }

    public static String sha256Hash(org.jsoup.nodes.Document doc) throws NoSuchAlgorithmException {
        String html = doc.html(); // Convert Document to HTML string
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(html.getBytes(StandardCharsets.UTF_8));

        // Convert to hex string
        BigInteger number = new BigInteger(1, hashBytes);
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros if needed
        while (hexString.length() < 64) {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }
}
