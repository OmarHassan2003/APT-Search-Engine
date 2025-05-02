package ranker;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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
  private static List<Map<String, Object>> docData;
  public static Snippeterr snippeter;
  private static String type;
  private static QueryResult res;
  private static DBManager database;

  public static List<RankedDocument> RankerMain(QueryResult q, DBManager db) {
    database = db;
    pageRankScores = db.getPageRank();
    snippeter = new Snippeterr();
    res = q;
    System.out.println("Result: " + q.getTotalCount());
    stemmedQueryWords = q.getQueryWords();
    originalQueryWords = q.getQueryWordsString();
    docData = q.getDocData();
    type = q.getType();

    RankDocuments();

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


  private static double calculateRelevance(List<String> queryWords, Map<String, Object> docDataForCurrWord, int docsWithWord) {
    double score = 0.0;
    for (String word : queryWords) {
      System.out.println(docsWithWord);
      double tfidf = (double)(docDataForCurrWord.get("tf")) * (1000 / docsWithWord);
      if (tfidf == 0.0) continue;
      List<String> positions = (List<String>)docDataForCurrWord.get("tags");
      if (positions.size() == 0) score += tfidf * getPositionalWeight("l");
      else {
        for (String position : positions) {
          score += getPositionalWeight(position) * tfidf;
        }
      }
    }
    return queryWords.size() > 0 ? score / queryWords.size() : 0.0;
  }

  private static double calcDifference(Map<String, Double> newRanks, Map<String, Double> oldRanks) {
    double sumOfDifferences = 0;
    for (String word : newRanks.keySet()) {
      sumOfDifferences += Math.abs(newRanks.get(word) - oldRanks.get(word));
    }
    return sumOfDifferences;
  }

  public static Map<String, Double> calculatePageRank(Map<String, List<String>> reverseGraph, DBManager database, Map<String, List<String>> URLGraph) {
    Map<String, Double> pageRank = new HashMap<>();
    double dampingFactor = 0.85;
    double prevIteration = 100;
    double convergenceThreshold = 0.001;

    for (String doc : URLGraph.keySet()) {
      pageRank.put(doc, (double) 1 / URLGraph.size());
    }

//    for (Map.Entry<String, List<String>> entry : URLGraph.entrySet()) {
//      System.out.println(entry.getKey());
//    }

    int iterations = 100;
    for (int i = 0; i < iterations; i++) { // mby change this, geeks says 100
      Map<String, Double> newRanks = new HashMap<>();
      double curr = 0;

      for (String targetPage : URLGraph.keySet()) {
        double contribution = 0.0;
        List<String> incomingLinks = reverseGraph.getOrDefault(targetPage, Collections.emptyList());
        for (String sourcePage: incomingLinks) {
          int outDegree = URLGraph.get(sourcePage).size();
          if (outDegree > 0) {
            contribution += pageRank.get(sourcePage) / outDegree;
          }
        }

      double newScore = (1 - dampingFactor) + dampingFactor * contribution;
      curr += newScore;
      newRanks.put(targetPage, newScore);
//        System.out.println("iteration " + i + " " + targetPage + " " + newScore);
      }


      double currDiff = calcDifference(newRanks, pageRank);
//      System.out.println(i + " " + currDiff);

      if (Math.abs(currDiff - prevIteration) < convergenceThreshold) {
        System.out.println("Convergence achieved at iteration: " + (i + 1));
        break;
      }

      System.out.println("iteration: " + i + " " + currDiff);
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

  public static void RankDocuments() {
    results = new ArrayList<>();
    System.out.println("Result: " + res.getTotalCount());
    for (int i = 0; i < res.getDocIds().size(); i++) {
      Map<String, Object> docDataForCurrWord = docData.get(i);
      double relevance = calculateRelevance(stemmedQueryWords, docDataForCurrWord, docDataForCurrWord.size());
      Document temp = database.getDocumentById(res.getDocIds().get(i));
      String url = (String)temp.get("url");
      String title = (String)temp.get("title");
      System.out.println(pageRankScores.size());
      double score = relevance * ((pageRankScores.get(url) != null) ? pageRankScores.get(url) : 1 / pageRankScores.size());
      String snippet = snippeter.generateSnippet((ArrayList<String>)temp.get("ps"), originalQueryWords);
      RankedDocument r = new RankedDocument(url, score, title, snippet);
      results.add(r);
    }

    Collections.sort(results, Comparator.comparingDouble(RankedDocument::getScore).reversed());
  }
}
