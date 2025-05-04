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
import processor.Stemmer;
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
  // document -> word -> position
  private static Map<String, Map<String, List<Integer>>> documentPositionsForEachWordTracker = new HashMap<>();

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

    if (!type.equals("phrase") && !type.equals("phrase+boolean"))
      RankDocuments();
    else PhraseAndBoolRank();
    return results;
  }

  private static double getPositionalWeight(String position) {
    return switch (position.toLowerCase()) {
      case "title" -> 4;
      case "h1" -> 2.5;
      case "h2" -> 2.0;
      case "h3" -> 1.5;
      case "h4", "h5", "h6" -> 1.0;
      default -> 0.5;
    };
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
    return score;
  }

  public static double calculateWeightedTF(double tf, List<String> positions) {
    double score = 0.0;
    if (positions.size() == 0) score += tf * getPositionalWeight("l");
    else {
      for (String position : positions) {
        score += getPositionalWeight(position) * tf;
      }
    }
    return score;
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
    Thread[] threads = new Thread[docData.keySet().size()];
    int i = 0;
    for (String word : docData.keySet()) {
      threads[i] = new Thread(() -> {
        Map<String, Object> docDataForCurrWord = docData.get(word);
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
          synchronized (scoreTracker) {
            if (scoreTracker.containsKey(url)) {
              RankedDocument tempp = scoreTracker.get(url);
              tempp.setScore(tempp.getScore() + score);
            }
            else {
              // need new logic for snippeting (check with tony nagy)
              List<String> paragraphs = (List<String>)temp.get("ps");
              System.out.println("paragraphs: " + paragraphs);
              String snippet = snippeter.generateSnippet(paragraphs, originalQueryWords);
              RankedDocument r = new RankedDocument(url, score, title, snippet);
              results.add(r);
              scoreTracker.put(url, r);
            }
          }
        }
      });
      threads[i++].start();
    }
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Collections.sort(results, Comparator.comparingDouble(RankedDocument::getScore).reversed());
  }

  // private static Map<String, Map<String, Object>> docData;
//  public static void PhraseRank(List<String> stemmedQueryWordsForEachIteration) {
//    results = new ArrayList<>();
//    Map<String, Object> initDocs = docData.get(stemmedQueryWords.getFirst());
//    Map<String, Object> wordsIntersection = initDocs;
//
//    for (String word : stemmedQueryWordsForEachIteration) {
//      Map<String, Object> docsForCurrWord = docData.get(word);
//      Map<String, Object> newIntersection = new HashMap<>();
//
//      for (String doc : docsForCurrWord.keySet()) {
//        Map<String, Object> docFields = (Map<String, Object>) docData.get(word).get(doc);
//        List<String> positionStrings = (List<String>) docFields.get("positions");
//        List<Integer> numberPositions = positionStrings == null ? new ArrayList<>() :
//                positionStrings.stream()
//                        .map(Integer::parseInt)
//                        .collect(Collectors.toList());
//
//        double tf = (double) docFields.get("tf");
//        Document temp = database.getDocumentById(doc);
//        String url = (String) temp.get("url");
//        String title = (String) temp.get("title");
//        List<String> positionsHTML = (List<String>) docFields.get("tags");
//        double tfWeighted = calculateWeightedTF(tf, positionsHTML);
//        if (tf == 0.0) continue;
//        double score = tfWeighted * (pageRankScores.get(url) != null ? pageRankScores.get(url) : 1 / pageRankScores.size());
//
//        if (scoreTracker.containsKey(url)) {
//          RankedDocument tempp = scoreTracker.get(url);
//          tempp.setScore(tempp.getScore() + score);
//        } else {
//          RankedDocument r = new RankedDocument(url, score, title, "");
//          scoreTracker.put(url, r);
//        }
//
//        if (!documentPositionsForEachWordTracker.containsKey(url)) {
//          documentPositionsForEachWordTracker.put(url, new HashMap<>());
//        }
//        documentPositionsForEachWordTracker.get(url).put(word, numberPositions);
//
//        for (String s : wordsIntersection.keySet()) {
//          if (doc.equals(s)) {
//            newIntersection.put(s, docsForCurrWord.get(s));
//            break;
//          }
//        }
//
//        wordsIntersection = newIntersection;
//      }
//    }
//
//      for (String docName : wordsIntersection.keySet()) {
//        int lastOccurence = -1;
//        boolean isPhrase = true;
//        for (String word : stemmedQueryWordsForEachIteration) {
//          boolean good = false;
//          for ()
//        }
//
//    }
//
//  }

  public static List<RankedDocument> PhraseRank(List<String> queryWordsCurr) {
    List<RankedDocument> localResults = new ArrayList<>();
    Map<String, RankedDocument> localScoreTracker = new HashMap<>();

    if (queryWordsCurr.isEmpty() || !docData.containsKey(queryWordsCurr.get(0))) {
      return localResults;
    }

    Map<String, Object> docsForFirstWord = docData.get(queryWordsCurr.get(0));
    for (String doc : docsForFirstWord.keySet()) {
      double totalScore = 0.0;
      Document temp = database.getDocumentById(doc);
      String url = (String) temp.get("url");
      String title = (String) temp.get("title");
      List<String> paragraphs = (List<String>) temp.get("ps");

      for (String word : queryWordsCurr) {
        Map<String, Object> docFields = (Map<String, Object>) docData.get(word).get(doc);
        double tf = (double) docFields.get("tf");
        List<String> positions = (List<String>) docFields.get("tags");
        double tfidf = calculateRelevance(docData.get(word).size(), tf, positions);
        totalScore += tfidf;
      }

      totalScore *= (pageRankScores.get(url) != null ? pageRankScores.get(url) : 1.0 / pageRankScores.size());
      if (totalScore == 0.0) continue;

      String snippet = snippeter.generateSnippet(paragraphs, queryWordsCurr);
      RankedDocument r = new RankedDocument(url, totalScore, title, snippet);
      localResults.add(r);
      localScoreTracker.put(url, r);
    }

    Collections.sort(localResults, Comparator.comparingDouble(RankedDocument::getScore).reversed());
    return localResults;
  }



//  public static void PhraseAndBoolRank() {
//    results = new ArrayList<>();
//    if (type.equals("phrase"))
//      results = PhraseRank(stemmedQueryWords);
//    else {
//      List<String> qw = new ArrayList<>();
//      String currSeperator = "";
//      // for (String word : originalQueryWords) {
//      for (int j = 0; j < originalQueryWords.size(); j++) {
//        String word = originalQueryWords.get(j);
//        if (!word.equalsIgnoreCase("AND") && !word.equalsIgnoreCase("OR") && !word.equalsIgnoreCase("NOT")) {
//          String stemmed = Stemmer.stem(word);
//          qw.add(stemmed);
//        } else {
//            if (word.equalsIgnoreCase("AND")) {
//              j++;
//              for (; j < originalQueryWords.size(); j++) {
//                qw.add(originalQueryWords.get(j));
//              }
//              results = PhraseRank(qw);
//            }
//            else if (word.equalsIgnoreCase("NOT")) {results = PhraseRank(qw);}
//            else {
//              scoreTracker = new HashMap<>();
//              for (RankedDocument doc : results) {
//                scoreTracker.put(doc.getUrl(), doc);
//              }
//              j++;
//              qw.clear();
//              for (; j < originalQueryWords.size(); j++) {
//                qw.add(originalQueryWords.get(j));
//              }
//              List<RankedDocument> tempResults = PhraseRank(qw);
//              for (RankedDocument r : tempResults) {
//                if (scoreTracker.containsKey(r.getUrl())) {
//                  RankedDocument existingDoc = scoreTracker.get(r.getUrl());
//                  existingDoc.setScore(existingDoc.getScore() + r.getScore());
//                } else {
//                  results.add(r);
//                  scoreTracker.put(r.getUrl(), r);
//                }
//              }
//            }
//        }
//      }
//    }
//    Collections.sort(results, Comparator.comparingDouble(RankedDocument::getScore).reversed());
//  }

  public static void PhraseAndBoolRank() {
    results = new ArrayList<>();
    scoreTracker = new HashMap<>();

    if (type.equals("phrase")) {
      results = PhraseRank(stemmedQueryWords);
      for (RankedDocument doc : results) {
        scoreTracker.put(doc.getUrl(), doc);
      }
      Collections.sort(results, Comparator.comparingDouble(RankedDocument::getScore).reversed());
      return;
    }

    List<String> queryWords = new ArrayList<>();
    for (String word : originalQueryWords) {
      if (!word.equalsIgnoreCase("AND") && !word.equalsIgnoreCase("OR") && !word.equalsIgnoreCase("NOT")) {
        String stemmed = Stemmer.stem(word);
        queryWords.add(stemmed);
      }
    }

    if (!queryWords.isEmpty()) {
      results = PhraseRank(queryWords);
      for (RankedDocument doc : results) {
        scoreTracker.put(doc.getUrl(), doc);
      }
    }

    Collections.sort(results, Comparator.comparingDouble(RankedDocument::getScore).reversed());
  }
}

