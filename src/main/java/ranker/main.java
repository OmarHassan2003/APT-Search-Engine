//package ranker;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class main {
//  public static void main(String[] args) {
////    private Map<String, Integer> wordOccurences;
////    private Map<String, List<String>> wordPositions;
////    private int totalWords;
////    private List<Document> referedToDocuments;
//    Map<String, Integer> wordOccurences1 = new HashMap<>();
//    wordOccurences1.put("Omar", 1);
//    wordOccurences1.put("Hassan", 2);
//    wordOccurences1.put("Tony", 3);
//    Map<String, List<String>> wordPositions1 = new HashMap<>();
//    wordPositions1.put("Omar", List.of("title"));
//    wordPositions1.put("Hassan", List.of("h1, h2"));
//    wordPositions1.put("Tony", List.of("h1, h2, body"));
//
//    Map<String, Integer> wordOccurences2 = new HashMap<>();
//    wordOccurences2.put("Omar", 1);
//    wordOccurences2.put("Hassan", 1);
//    wordOccurences2.put("Tony", 4);
//    Map<String, List<String>> wordPositions2 = new HashMap<>();
//    wordPositions2.put("Omar", List.of( "h1"));
//    wordPositions2.put("Hassan", List.of("h1"));
//    wordPositions2.put("Tony", List.of("h3", "h4", "body", "h2"));
//
//    Map<String, Integer> wordOccurences3 = new HashMap<>();
//    wordOccurences3.put("Omar", 2);
//    wordOccurences3.put("Magdy", 1);
//    wordOccurences3.put("Nabil", 4);
//    Map<String, List<String>> wordPositions3 = new HashMap<>();
//    wordPositions3.put("Omar", List.of("h3", "h4"));
//    wordPositions3.put("Magdy", List.of("h1"));
//    wordPositions3.put("Nabil", List.of("h5", "h3", "h4", "body"));
//
//    Documents doc1 = new Documents("www.f1.com", "Greatest Motorsport ever", wordOccurences1 , wordPositions1);
//    Documents doc2 = new Documents("www.f2.com", "2nd Greatest Motorsport ever", wordOccurences2 , wordPositions2);
//    Documents doc3 = new Documents("www.f13.com", "3rd Greatest Motorsport ever", wordOccurences3 , wordPositions3);
//
//    List<Documents> referedTo1 = List.of(doc2, doc3);
//    List<Documents> referedTo2 = List.of(doc3, doc1);
//    List<Documents> referedTo3 = List.of(doc1);
//
//    doc1.setReferedTo(referedTo1);
//    doc2.setReferedTo(referedTo2);
//    doc3.setReferedTo(referedTo3);
//
//    Documents[] tot = {doc1, doc2, doc3};
//    Map<String, Integer> docsWithWord = new HashMap<>();
//    docsWithWord.put("Omar", 3);
//    docsWithWord.put("Magdy", 1);
//    docsWithWord.put("Nabil", 1);
//    docsWithWord.put("Tony", 2);
//    docsWithWord.put("Hassan", 2);
//
//    Ranker ranker = new Ranker(tot, docsWithWord);
//
//    String[] query = {"Omar"};
//    Documents[] docs = {doc1, doc2, doc3};
//    List<Documents> d = ranker.RankDocuments(query, docs);
//
//    for (Documents doc : d) {
//      System.out.println(doc.getUrl());
//    }
//  }
//}
