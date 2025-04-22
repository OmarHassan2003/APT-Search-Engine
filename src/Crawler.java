package src;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Map;
import java.util.regex.Pattern;
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
    private final Set<String> roboSet;
    private final Map<String, List<Pattern>> unAllowedURLs;
    private static final int MAX_PAGES = 100;

    public Crawler(List<String> urls, int id, Set<String> visitedURLs, List<Document> results,
            Queue<String> urlFrontier, Set<String> roboSet,
            Map<String, List<Pattern>> unAllowedURLs) {
        this.roboSet = roboSet;
        this.unAllowedURLs = unAllowedURLs;
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
                synchronized (visitedURLs) {
                    if (visitedURLs.contains(url))
                        continue;
                    visitedURLs.add(url);
                }
                synchronized (urlFrontier) {
                    urlFrontier.add(url);
                }

            }
            crawl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void crawl() {
        while (results.size() < MAX_PAGES) {
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
            String base = null;
            try {
                URL baseURL = URI.create(url).toURL();
                base = baseURL.getProtocol() + "://" + baseURL.getHost();
                if (!roboSet.contains(base)) {
                    List<Pattern> disallowed = RobotParser.parse(base);
                    if (disallowed != null) {
                        synchronized (roboSet) {
                            roboSet.add(base);
                        }
                        synchronized (unAllowedURLs) {
                            unAllowedURLs.put(base, disallowed);
                        }
                    }

                }
            } catch (Exception e) {
                System.out.println("Error parsing URL: " + e.getMessage());
            }
            if (base != null && roboSet.contains(base)) {
                List<Pattern> disallowed = unAllowedURLs.get(base);
                if (disallowed != null) {
                    if (RobotParser.isDisallowed(url, disallowed)) {
                        continue;
                    }
                }
            }
            Document doc = downloadPage(url);
            if (doc == null) {
                continue;
            }
            synchronized (results) {
                results.add(doc);
            }
            List<String> links = extractLinks(doc);
            try {
                for (String link : links) {
                    link = URLNormalizer.normalizeToCompactString(link);
                    if (link == null) {
                        continue;
                    }
                    if (!visitedURLs.contains(link)) {
                        synchronized (visitedURLs) {
                            if (visitedURLs.contains(link)) // add to visited when adding to queue,
                                                            // not when poping from queue to ensure
                                                            // no duplicates
                                continue;
                            visitedURLs.add(link);
                        }
                        synchronized (urlFrontier) {
                            urlFrontier.add(link);
                        }
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
            if (normalizedURL != null) {
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
