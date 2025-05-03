package ranker;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Comparator;
import org.bson.Document;
import processor.QueryResult;
import db.DBManager;

public class Ranker {
  public static List<String> stemmedQueryWords;
  public static List<String> originalQueryWords;
  public static Map<String, Double> pageRankScores;
  private static List<RankedDocument> results;
  private static Map<String, Map<String, Object>> docData;
  private static Map<String, RankedDocument> scoreTracker;
  public static Snippeterr snippeter;
  private static String type;
  private static QueryResult res;
  private static DBManager database;

  public static List<RankedDocument> RankerMain(QueryResult q, DBManager db) {
    database = db;
    pageRankScores = db.getPageRank();
    snippeter = new Snippeterr();
    res = q;
    //System.out.println("Result: " + q.getTotalCount());
    stemmedQueryWords = q.getQueryWords();
    originalQueryWords = q.getQueryWordsString();
    docData = q.getPerWordResults();
    type = q.getType();
    scoreTracker = new HashMap<>();

    if (!type.equals("phrase"))
      RankDocuments();
    else {
      // call the phrase rank method
    }
    return results;
  }

  private static double getPositionalWeight(String position) {
    switch (position.toLowerCase()) {
      case "title":
        return 4;
      case "h1":
        return 2.5;
      case "h2":
        return 2.0;
      case "h3":
        return 1.5;
      case "h4":
      case "h5":
      case "h6":
        return 1.0;
      default:
        return 0.5;
    }
  }


  private static double calculateRelevance(int docsWithWord, double tf, List<String> positions) {
    double score = 0.0;
    //tem.out.println(docsWithWord);
    double tfidf = (tf) * (6000 / docsWithWord);
    if (positions.size() == 0) score += tfidf * getPositionalWeight("l");
    else {
      for (String position : positions) {
        score += getPositionalWeight(position) * tfidf;
      }
    }
    return tfidf;
  }

  private static double calcDifference(Map<String, Double> newRanks, Map<String, Double> oldRanks) {
    double sumOfDifferences = 0;
    for (String word : newRanks.keySet()) {
      sumOfDifferences += Math.abs(newRanks.get(word) - oldRanks.get(word));
    }
    return sumOfDifferences;
  }

  public static Map<String, Double> calculatePageRank(Map<String, List<String>> reverseGraph, Map<String, List<String>> URLGraph) {
    Map<String, Double> pageRank = new HashMap<>();
    double dampingFactor = 0.85;
    double prevIteration = 100;
    double convergenceThreshold = 0.001;
    int N = URLGraph.size();

    for (String doc : URLGraph.keySet()) {
      pageRank.put(doc, 1.0 / N);
    }

    Map<String, Integer> outLinkCounts = new HashMap<>();
    Set<String> knownPages = URLGraph.keySet();

    for (String page : knownPages) {
      List<String> outLinks = URLGraph.get(page);
      int validCount = 0;
      if (outLinks != null) {
        for (String link : outLinks) {
          if (knownPages.contains(link)) {
            validCount++;
          }
        }
      }
      outLinkCounts.put(page, validCount);
    }

    int iterations = 100;
    for (int i = 0; i < iterations; i++) {
      Map<String, Double> newRanks = new HashMap<>();

      double danglingRank = 0.0;
      for (String page : knownPages) {
        if (outLinkCounts.getOrDefault(page, 0) == 0) {
          danglingRank += pageRank.get(page);
        }
      }

      for (String targetPage : knownPages) {
        double contribution = 0.0;
        List<String> incomingLinks = reverseGraph.getOrDefault(targetPage, Collections.emptyList());
        for (String sourcePage : incomingLinks) {
          int outCount = outLinkCounts.getOrDefault(sourcePage, 0);
          if (outCount > 0) {
            contribution += pageRank.get(sourcePage) / outCount;
          }
        }

        double newScore = (1 - dampingFactor) / N + dampingFactor * (contribution + danglingRank / N);
        newRanks.put(targetPage, newScore);
      }

      double currDiff = calcDifference(newRanks, pageRank);
      System.out.println("iteration: " + i + " " + currDiff);

      if (Math.abs(currDiff - prevIteration) < convergenceThreshold) {
        System.out.println("Convergence achieved at iteration: " + (i + 1));
        break;
      }

      pageRank = newRanks;
      prevIteration = currDiff;
    }

    return pageRank;
  }



  // travel guide


  // query result: {
  // docIds: [[1, 2, 3], [2, 3]]
  // docData: [ [ {tf1, positions1, tag1}, {tf2, positions2, tag2}, {3} ], [ {tf1, positions1, tag1}, {tf2, positions2, tag2} ] ]
  // queryWords: ["travel", "guide"]
// }
  // {travel, guide)


  // private static Map<String, Map<String, Object>> docData;
  public static void RankDocuments() {
    results = new ArrayList<>();
    for (String word : docData.keySet()) {
      Map<String, Object> docDataForCurrWord = docData.get(word);
        //tem.out.println("word: " + word);
      for (String doc : docDataForCurrWord.keySet()) {
        Map<String, Object> docFields = (Map<String, Object>) docData.get(word).get(doc);
        double tf = (double)docFields.get("tf");
        Document temp = database.getDocumentById(doc);
        String url = (String)temp.get("url");
        String title = (String)temp.get("title");
        List<String> positions = (List<String>)docFields.get("tags");
        double tfidf = calculateRelevance(docDataForCurrWord.size(), tf, positions);
        if (tfidf == 0.0) continue;
        double score = tfidf * (pageRankScores.get(url) != null ? pageRankScores.get(url) : 1 / pageRankScores.size());
        if (scoreTracker.containsKey(doc)) {
          RankedDocument tempp = scoreTracker.get(url);
          tempp.setScore(tempp.getScore() + score);
        }
        else {
          // need new logic for snippeting (check with tony nagy)
          RankedDocument r = new RankedDocument(url, score, title, "ana baheb tony nagy");
          results.add(r);
          scoreTracker.put(url, r);
        }
      }
    }

    Collections.sort(results, Comparator.comparingDouble(RankedDocument::getScore).reversed());
  }
}
