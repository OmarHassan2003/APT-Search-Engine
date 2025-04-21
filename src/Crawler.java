package src;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.jsoup.nodes.Element;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.Connection;

public class Crawler implements Runnable {

    private final List<String> seedURLs;
    private final Queue<String> urlFrontier;
    private int ID = 0;
    private final List<Document> results;
    private final Set<String> visitedURLs;
    private static final int MAX_PAGES = 100;
    private static final int MAX_THREADS = 10;

    public Crawler(List<String> urls, int id, Set<String> visitedURLs, List<Document> results,
            Queue<String> urlFrontier) {
        this.seedURLs = urls;
        this.urlFrontier = urlFrontier;
        this.ID = id;
        this.visitedURLs = visitedURLs;
        this.results = results;
    }


    @Override
    public void run() {
        try {
            for (String url : seedURLs) {
                url = URLNormalizer.normalizeToCompactString(url);
                urlFrontier.add(url);

            }
            crawl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void crawl() {
        while (results.size() < MAX_PAGES) {
            String url;
            url = urlFrontier.poll();
            if (url == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
                continue;
            }
            if (!visitedURLs.add(url)) {
                continue;
            }
            // Mark as visited BEFORE downloading to prevent other threads from processing
            Document doc = downloadPage(url);
            if (doc == null) {
                continue;
            }
            results.add(doc);
            List<String> links = extractLinks(doc);
            try {
                for (String link : links) {
                    link = URLNormalizer.normalizeToCompactString(link);
                    if (link == null) {
                        continue; // Skip null links
                    }
                    if (!visitedURLs.contains(link)) {
                        urlFrontier.add(link);
                    }
                }
            } catch (URISyntaxException e) {
                System.out.println("Error normalizing URL: " + e.getMessage());
            }
        }
        System.out.println(results.size() + " pages" + " crawled by thread " + ID);
    }

    private List<String> extractLinks(Document doc) {
        List<String> links = new ArrayList<>();
        for (Element link : doc.select("a[href]")) {
            String absURL = link.attr("abs:href");

            String normalizedURL = (absURL);
            if (normalizedURL != null) { // Only add if not null
                links.add(normalizedURL);
            }
        }
        return links;
    }

    private Document downloadPage(String url) {
        try {
            Connection connection = Jsoup.connect(url).timeout(10000) // Add a reasonable timeout
                    .followRedirects(true);

            Document doc = connection.get();
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
}
