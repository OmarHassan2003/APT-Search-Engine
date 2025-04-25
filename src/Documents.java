package src;

import java.util.*;

public class Documents {
  private String url;
  private String title;
  private Map<String, Integer> wordOccurences;
  private Map<String, List<String>> wordPositions;
  private int totalWords;
  private List<Documents> referedToDocuments;

  public Documents(String url, String title, Map<String, Integer> wordOccurences, Map<String, List<String>> wordPositions) {
    this.url = url;
    this.title = title;
    this.wordOccurences = wordOccurences;
    this.wordPositions = wordPositions;

    for(Map.Entry<String, Integer> entry : wordOccurences.entrySet()) {
      totalWords += entry.getValue();
    }

    System.out.println("total words " + totalWords);
  }

  public void setReferedTo(List<Documents> referedTo) {
    this.referedToDocuments = referedTo;
  }

  public String getUrl() {return url;}

  public int getTotalWords() {return totalWords;}

  public int getWordsOccurences(String word) {return wordOccurences.getOrDefault(word, 0);}

  public List<String> getWordPositions(String word) {return wordPositions.getOrDefault(word, Collections.emptyList());}

  public List<Documents> getReferedTo() {return referedToDocuments;}
}
