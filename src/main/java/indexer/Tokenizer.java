package indexer;

import processor.Stemmer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.bson.Document;

public class Tokenizer {

   public class Token {
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
       loadStopWords("../data/stopwords.txt");
   }

   private void loadStopWords(String filePath) {
       try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
           String line;
           while ((line = reader.readLine()) != null) {
               stopWords.add(line.trim().toLowerCase());
           }
       } catch (IOException e) {
           System.err.println("Error loading stop words: " + e.getMessage());
       }
   }

   public HashMap<String, Token> tokenizeDoc(Document doc) {
       String text = doc.getString("content");
       if (text == null) {
           System.out.println("Document content is null");
           return new HashMap<>();
       }

       List<String> tokens = tokenizeString(text);
       HashMap<String, Token> tokenMap = new HashMap<>();

       // Use parallel streams to process tokens
       tokens.parallelStream().forEach(token -> {
           if (token.isBlank() || stopWords.contains(token)) return;

           synchronized (tokenMap) { // Synchronize access to the shared map
               if (tokenMap.containsKey(token)) {
                   tokenMap.get(token).increment();
               } else {
                   tokenMap.put(token, new Token(token));
               }
           }
       });

       // Calculate term frequency (TF)
       tokens.parallelStream().forEach(token -> {
           synchronized (tokenMap) {
               if (tokenMap.containsKey(token)) {
                   tokenMap.get(token).count = tokenMap.get(token).count / tokens.size();
               }
           }
       });

       fillTags(doc, tokenMap);
       return tokenMap;
   }

   public List<String> tokenizeString(String text) {
       List<String> tokens = new ArrayList<>();
       String currentText = text.toLowerCase().replaceAll("[^a-z0-9]", " "); // Include numbers
       String[] words = currentText.split("\\s+");

       for (String word : words) {
           if (!word.isBlank()) {
               tokens.add(Stemmer.stem(word)); 
           }
       }

       return tokens;
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


