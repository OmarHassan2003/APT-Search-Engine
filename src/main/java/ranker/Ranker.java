package ranker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import processor.QueryResult;

public class Ranker {
  public static List<String> stemmedQueryWords;
  public static List<String> originalQueryWords;
  public static Map<String, Double> pageRankScores;
  private static Map<String, RankedDocument> results;
  private static List<Map<String, Object>> docData;
  private static String type;
  private static QueryResult q;

  public Map<String, RankedDocument> RankerMain(Map<String, Double> pageRankScores, QueryResult q) {
    this.pageRankScores = pageRankScores;
    this.q = q;
    this.stemmedQueryWords = q.getQueryWords();
    this.originalQueryWords = q.getQueryWordsString();
    this.docData = q.getDocData();
    this.type = q.getType();

    RankDocuments(pageRankScores);

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
      double tfidf = (double)(docDataForCurrWord.get("tf")) * (109.0 / docsWithWord);
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

  private static double diff(Map<String, Double> newRanks, Map<String, Double> oldRanks) {
    int sumOfDifferences = 0;
    for (String word : newRanks.keySet()) {
      sumOfDifferences += Math.abs(newRanks.get(word) - oldRanks.get(word));
    }
    return sumOfDifferences;
  }

  private static Map<String, Double> calculatePageRank(Map<String, List<String>> URLGraph) {
    Map<String, Double> pageRank = new HashMap<>();
    double dampingFactor = 0.85;

    double prevIteration = 1000;
    double convergenceThreshold = 0.01;

    for (String doc : URLGraph.keySet()) {
      pageRank.put(doc, (double) 1 / URLGraph.size());
    }

    int iterations = 100;
    for (int i = 0; i < iterations; i++) { // mby change this, geeks says 100
      Map<String, Double> newRanks = new HashMap<>();
      double curr = 0;
      for (String doc1 : URLGraph.keySet()) {
        double contribution = 0.0;
        if (URLGraph.get(doc1) != null && !URLGraph.get(doc1).isEmpty()) {
          for (String doc2 : URLGraph.get(doc1)) {
            if (URLGraph.containsKey(doc1)) {
              contribution += pageRank.get(doc2) / URLGraph.get(doc2).size();
            }
          }
        }
        double newScore = (1 - dampingFactor) + dampingFactor * contribution;
        curr += newScore;
        newRanks.put(doc1, newScore);
      }

      // for (Map.Entry<String, Double> entry : newRanks.entrySet()) {
      //  System.out.println(entry.getKey().getUrl() + " " + entry.getValue());
      // }
      System.out.println("-------------------------");

      double currDiff = diff(newRanks, pageRankScores);

      if (Math.abs(currDiff - prevIteration) < convergenceThreshold) {
        System.out.println("Convergence achieved at iteration: " + (i + 1));
        break;
      }

      pageRank = newRanks;
      prevIteration = curr;
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

  public static Map<String, RankedDocument> RankDocuments(Map<String, Double> pageRankScores) {
    results = new HashMap<>();

    for (int i = 0; i < q.getDocIds().size(); i++) {
      Map<String, Object> docDataForCurrWord = docData.get(i);
      double relevance = calculateRelevance(stemmedQueryWords, docDataForCurrWord, docDataForCurrWord.size());
      // double score = 0.6 * relevance + 0.4 * pageRankScores.get()
    }
    return results;
  }
}
