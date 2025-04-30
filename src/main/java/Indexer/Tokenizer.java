package Indexer;

import processor.Stemmer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.bson.Document;

public class Tokenizer {

   public static class Token { // Made static
       public String word;
       public double count;
       public List<Integer> positions;
       public List<String> tags;

       public Token(String word) {
           this.word = word;
           this.count = 1;
           this.positions = new ArrayList<>();
           this.tags = new ArrayList<>();
       }

       public void increment() {
           this.count++;
       }

       public void addPosition(int pos) {
           this.positions.add(pos);
       }
   }

   private Set<String> stopWords;

   public Tokenizer() {
       stopWords = new HashSet<>(); // Initialize stopWords
       loadStopWords("./src/main/java/data/stopwords.txt"); // Use relative path
   }

   private void loadStopWords(String filePath) {
       try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
           String line;
           while ((line = reader.readLine()) != null) {
               stopWords.add(line.trim().toLowerCase());
           }
       } catch (IOException e) {
           System.err.println("Error loading stop words from " + filePath + ": " + e.getMessage());
       }
   }

   public HashMap<String, Token> tokenizeDoc(Document doc) {
       String text = doc.getString("body");
       if (text == null || text.isBlank()) {
           System.err.println("[ERROR] Document body is null or empty for document: " + doc.getString("title"));
           return new HashMap<>();
       }

       HashMap<String, Token> tokenMap = new HashMap<>();
       String[] words = text.toLowerCase().replaceAll("[^a-z0-9]", " ").split("\\s+");
       
       for (int i = 0; i < words.length; i++) {
           String word = words[i];
           
           // Skip invalid tokens (too short, numbers, stopwords)
           if (word.length() <= 1 || word.matches("\\d+") || stopWords.contains(word)) {
               continue;
           }
           
           try {
               String stemmedWord = Stemmer.stem(word);
               if (stemmedWord.isBlank()) continue;
               
               if (tokenMap.containsKey(stemmedWord)) {
                   tokenMap.get(stemmedWord).increment();
                   tokenMap.get(stemmedWord).addPosition(i);
               } else {
                   Token token = new Token(stemmedWord);
                   token.addPosition(i);
                   tokenMap.put(stemmedWord, token);
               }
           } catch (Exception e) {
               System.err.println("[ERROR] Error processing word: " + word + " - " + e.getMessage());
           }
       }
       
       // Calculate term frequency (TF)
       int totalTerms = tokenMap.values().stream().mapToInt(t -> (int)t.count).sum();
       if (totalTerms > 0) {
           tokenMap.forEach((key, token) -> token.count = token.count / totalTerms);
       }
       
       fillTags(doc, tokenMap);
       return tokenMap;
   }

   public void fillTags(Document doc, HashMap<String, Token> tokenMap) {
       for (String token : tokenMap.keySet()) {
           Token t = tokenMap.get(token);

           String title = doc.getString("title");
           if (title != null && title.toLowerCase().contains(token)) {
               t.tags.add("title");
           }

           List<String> h1s = doc.getList("h1s", String.class);
           if (h1s != null) {
               for (String h1 : h1s) {
                   if (h1.toLowerCase().contains(token)) {
                       t.tags.add("h1");
                       break;
                   }
               }
           }

           List<String> h2s = doc.getList("h2s", String.class);
           if (h2s != null) {
               for (String h2 : h2s) {
                   if (h2.toLowerCase().contains(token)) {
                       t.tags.add("h2");
                       break;
                   }
               }
           }

           List<String> h3s = doc.getList("h3s", String.class);
           if (h3s != null) {
               for (String h3 : h3s) {
                   if (h3.toLowerCase().contains(token)) {
                       t.tags.add("h3");
                       break;
                   }
               }
           }

           List<String> h456s = doc.getList("h456s", String.class);
           if (h456s != null) {
               for (String h456 : h456s) {
                   if (h456.toLowerCase().contains(token.toLowerCase())) {
                       t.tags.add("h456");
                       break;
                   }
               }
           }

           String body = doc.getString("body");
           if (body != null && body.toLowerCase().contains(token)) {
               t.tags.add("body");
           }
       }
   }
}


