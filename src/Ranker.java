package src;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Ranker {
  private Document[] totalDocs;
  private Map<String, Integer> docsWithWord; // word->no of docs containing it

  public Ranker(Document[] totalDocs, Map<String, Integer> docsWithWord) {
    this.totalDocs = totalDocs;
    this.docsWithWord = docsWithWord;
  }

  private double getPositionalWeight(String position) {
    switch (position.toLowerCase()) {
      case "title": return 3;
      case "h1": return 2.5;
      case "h2": return 2.0;
      case "h3": return 1.5;
      case "h4":
      case "h5":
      case "h6": return 1.0;
      default: return 0.5;
    }
  }

  private double calculateTFIDF(String word, Document doc) {
    double tf =  (double)doc.getWordsOccurences(word) /doc.getTotalWords();
    double idf = (double) totalDocs.length / (docsWithWord.getOrDefault(word, 1));
    return tf * idf;
  }

  private double calculateRelevance(String[] queryWords, Document doc) {
    double score = 0.0;
    for(String word : queryWords) {
      double tfidf = calculateTFIDF(word, doc);
      if (tfidf == 0.0) continue;
      List<String> positions = doc.getWordPositions(word);
      if (positions.size() == 0) score += tfidf * getPositionalWeight("l");
      else {
        for (String position : positions) {
          score += getPositionalWeight(position) * tfidf;
        }
      }
    }
    return queryWords.length > 0 ? score / queryWords.length : 0.0;
  }

    private Map<Document, Double> calculatePageRank() {
      Map<Document, Double> pageRank = new HashMap<>();
      double dampingFactor = 0.85;
      for(Document doc : totalDocs) {
        pageRank.put(doc, (double)1 / totalDocs.length);
      }
      for(int i = 0; i < 5; i++) { // mby change this, geeks says 100
        Map<Document, Double> newRanks = new HashMap<>();
        for(Document doc1 : totalDocs) {
          double contribution = 0.0;
          for(Document doc2 : totalDocs) {
            if (!(doc1.getUrl().equals(doc2.getUrl()))) {
              Document From = doc2;
              if(doc2.getReferedTo().contains(doc1)) {
                contribution += pageRank.get(From) / doc2.getReferedTo().size();
              }
            }
          }
          double newScore = (1 - dampingFactor) + dampingFactor * contribution;
          newRanks.put(doc1, newScore);
        }
        for(Map.Entry<Document, Double> entry : newRanks.entrySet()) {
          System.out.println(entry.getKey().getUrl() + " " + entry.getValue());
        }
        System.out.println("-------------------------");
        pageRank = newRanks;
      }
      return pageRank;
    }

  public List<Document> RankDocuments(String[] queryWords, Document[] Docs) {
    Map<Document, Double> FinalScores = new HashMap<>();
    Map<Document, Double> pageRankedDocs = calculatePageRank();
    for (Document doc : Docs) {
      double relevance = calculateRelevance(queryWords, doc);
      System.out.println(relevance + " " + pageRankedDocs.get(doc));
      double score = 0.6 * relevance + 0.4 * pageRankedDocs.get(doc);
      FinalScores.put(doc, score);
    }

    return FinalScores.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
  }
}
