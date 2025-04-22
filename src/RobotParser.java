package src;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

public class RobotParser {

    public static List<Pattern> parse(String urlStr) {
        List<Pattern> disallowedPatterns = new ArrayList<>();

        try {
            String robotsUrl = urlStr + "/robots.txt";
            URL robotsFileURL = URI.create(robotsUrl).toURL();

            BufferedReader in =
                    new BufferedReader(new InputStreamReader(robotsFileURL.openStream()));
            String line;
            boolean appliesToUs = false;

            while ((line = in.readLine()) != null) {
                line = line.trim();

                if (line.toLowerCase().startsWith("user-agent:")) {
                    String agent = line.substring("user-agent:".length()).trim();
                    appliesToUs = agent.equals("*");
                }

                if (appliesToUs && line.toLowerCase().startsWith("disallow:")) {
                    String path = line.substring("disallow:".length()).trim();
                    if (!path.isEmpty()) {
                        String regex = convertToRegex(path);
                        disallowedPatterns.add(Pattern.compile(regex));
                    }
                }
            }

            in.close();
        } catch (Exception e) {
            System.out.println("robots.txt not found or failed to parse for: " + urlStr);
        }
        return disallowedPatterns;
    }

    private static String convertToRegex(String disallowPath) {
        // Escape special regex characters
        String regex = disallowPath.replaceAll("([.?+^$\\[\\]\\\\(){}|])", "\\\\$1");
        regex = regex.replace("*", ".*");
        if (!regex.startsWith("/")) {
            regex = "/" + regex;
        }
        return ".*" + regex + ".*"; // match anywhere in the URL
    }

    public static boolean isDisallowed(String url, List<Pattern> disallowedPatterns) {
        for (Pattern pattern : disallowedPatterns) {
            if (pattern.matcher(url).find()) {
                return true;
            }
        }
        return false;
    }
}
