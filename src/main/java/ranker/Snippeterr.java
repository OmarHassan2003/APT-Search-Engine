package ranker;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Snippeterr {
  private static final int SNIPPET_LENGTH = 1000;
  private static final String HIGHLIGHT_START = "<b>";
  private static final String HIGHLIGHT_END = "</b>";

  private int numberOfTerms;

  public String generateSnippet(List<String> paragraphs, List<String> queryWords) {

    String bestSnippet = "";
    double bestScore = -1.0;
    double bestNumberOfTerms = -1.0;

    if (paragraphs == null || paragraphs.isEmpty()) {
      return bestSnippet;
    }


    for (String paragraph : paragraphs) {
      int score = calculateRelevanceScore(paragraph, queryWords);

      if (paragraph.length() <= SNIPPET_LENGTH && (numberOfTerms > bestNumberOfTerms || numberOfTerms == bestNumberOfTerms && score > bestScore)) {
        System.out.println(score);
        bestSnippet = paragraph;
        bestScore = score;
        bestNumberOfTerms = numberOfTerms;
        System.out.println(bestSnippet);
      }
    }

    for (String term : queryWords) {
      System.out.println(term);
      bestSnippet = highlightQueryTerm(bestSnippet, term);
    }
    System.out.println(bestSnippet);
    return bestSnippet;
  }

  private int calculateRelevanceScore(String text, List<String> queryWords) {
    int score = 0;
    numberOfTerms = 0;
    for (String term : queryWords) {
      if (term == null || term.isEmpty()) continue;
      int frequency = countFrequency(text.toLowerCase(), term.toLowerCase());
      score += frequency;
      if (frequency != 0) numberOfTerms++;
    }

    return score;
  }

  private int countFrequency(String text, String term) {
    String escapedTerm = Pattern.quote(term);

    Pattern pattern = Pattern.compile(escapedTerm, Pattern.CASE_INSENSITIVE);

    Matcher matcher = pattern.matcher(text);

    int frequency = 0;
    while (matcher.find()) {
      frequency++;
    }
    return frequency;
  }

  private String highlightQueryTerm(String snippet, String term) {
    if (term == null || term.isEmpty()) return snippet;

    String escapedTerm = Pattern.quote(term);
    Pattern pattern = Pattern.compile(escapedTerm, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(snippet);

    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(sb, HIGHLIGHT_START + matcher.group() + HIGHLIGHT_END);
    }
    matcher.appendTail(sb);

    return sb.toString();
  }

}