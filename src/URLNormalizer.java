package src;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class URLNormalizer {

    public static String normalizeToCompactString(String url) throws URISyntaxException {
        if (url == null || url.isEmpty()) {
            return "";
        }

        // Ensure URL has a scheme, assuming http if missing
        if (!url.contains("://")) {
            url = "http://" + url;
        }

        URI uri = new URI(url);
        String scheme = uri.getScheme().toLowerCase();
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        String query = uri.getQuery();

        if (host != null) {
            host = host.toLowerCase();

            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
        }

        // Handle default ports
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            port = -1;
        }

        // Path normalization
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        // Remove trailing slash if not the root
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        String[] indexFiles =
                {"/index.html", "/index.htm", "/index.php", "/default.html", "/default.htm"};
        for (String indexFile : indexFiles) {
            if (path.endsWith(indexFile)) {
                path = path.substring(0, path.length() - indexFile.length());
                if (path.isEmpty()) {
                    path = "/";
                }
                break;
            }
        }

        // Handle path with . and .. segments
        path = normalizePath(path);

        if (query != null && !query.isEmpty()) {
            query = normalizeQueryParameters(query);

            // Remove empty query parameters
            if (query.contains("=&") || query.endsWith("=")) {
                query = query.replaceAll("([^&=]+)=(?:&|$)", "$1&");
                query = query.replaceAll("&$", "");
                if (query.isEmpty()) {
                    query = null;
                }
            }
        }

        // Build compact string representation (without fragment)
        StringBuilder compactUrl = new StringBuilder();
        compactUrl.append(scheme).append("://");
        compactUrl.append(host);

        if (port != -1) {
            compactUrl.append(":").append(port);
        }

        compactUrl.append(path);

        if (query != null && !query.isEmpty()) {
            compactUrl.append("?").append(query);
        }

        return compactUrl.toString().intern(); // using compact string
    }

    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // Replace multiple slashes with a single slash
        path = path.replaceAll("//+", "/");

        // Handle . and .. segments
        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            } else if ("..".equals(segment)) {
                if (!segments.isEmpty()) {
                    segments.remove(segments.size() - 1);
                }
            } else {
                segments.add(segment);
            }
        }

        StringBuilder normalizedPath = new StringBuilder();
        normalizedPath.append("/");
        if (!segments.isEmpty()) {
            for (String segment : segments) {
                normalizedPath.append(segment).append("/");
            }
            normalizedPath.setLength(normalizedPath.length() - 1);
        }

        return normalizedPath.toString();
    }

    // sort query parameters in alphabetical order
    private static String normalizeQueryParameters(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }

        String[] params = query.split("&");
        List<String> sortedParams = new ArrayList<>();
        Collections.addAll(sortedParams, params);
        Collections.sort(sortedParams);

        StringBuilder normalizedQuery = new StringBuilder();
        for (String param : sortedParams) {
            if (normalizedQuery.length() > 0) {
                normalizedQuery.append("&");
            }
            normalizedQuery.append(param);
        }

        return normalizedQuery.toString();
    }
}
