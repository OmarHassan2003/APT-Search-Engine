package Indexer;

import processor.Stemmer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.bson.Document;

public class Tokenizer {

   public static class Token {
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
   private static final Pattern NON_ALPHA_NUMERIC = Pattern.compile("[^a-z0-9]");
   private static final Pattern WHITESPACE = Pattern.compile("\\s+");
   private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");
   private final Map<String, String> stemCache = new ConcurrentHashMap<>(10000);

   private String processHeadingText(String text) {
       StringBuilder result = new StringBuilder();
       String[] words = WHITESPACE.split(NON_ALPHA_NUMERIC.matcher(text.toLowerCase()).replaceAll(" "));

       for (String word : words) {
           if (word.length() <= 1 || DIGITS_ONLY.matcher(word).matches() || stopWords.contains(word)) {
               continue;
           }

           try {
               String stemmedWord = Stemmer.stem(word);
               if (!stemmedWord.isBlank()) {
                   if (result.length() > 0) {
                       result.append(" ");
                   }
                   result.append(stemmedWord);
               }
           } catch (Exception e) {
               System.err.println("Error stemming word: " + word + " - " + e.getMessage());
           }
       }

       return result.toString();
   }

   public Tokenizer() {
       stopWords = new HashSet<>();
       loadStopWords("./src/main/java/data/stopwords.txt");
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
           return new HashMap<>();
       }

       HashMap<String, Token> tokenMap = new HashMap<>();
       String[] words = WHITESPACE.split(NON_ALPHA_NUMERIC.matcher(text.toLowerCase()).replaceAll(" "));

       for (int i = 0; i < words.length; i++) {
           String word = words[i];

           if (word.length() <= 1 || DIGITS_ONLY.matcher(word).matches() || stopWords.contains(word)) {
               continue;
           }
           String stemmedWord = stemCache.computeIfAbsent(word, w -> {
               try {
                   return Stemmer.stem(w);
               } catch (Exception e) {
                   return "";
               }
           });

            if (stemmedWord.isEmpty()) continue;

            Token token = tokenMap.computeIfAbsent(stemmedWord, k -> new Token(k));
            token.increment();
            token.addPosition(i);
       }

       int totalTerms = tokenMap.values().stream().mapToInt(t -> (int)t.count).sum();
       if (totalTerms > 0) {
           tokenMap.forEach((key, token) -> token.count = token.count / totalTerms);
       }

       fillTags(doc, tokenMap);
       return tokenMap;
   }

   private int countWordOccurrences(String text, String word) {
       int count = 0;
       int wordLength = word.length();
       int pos = 0;

       while ((pos = text.indexOf(word, pos)) >= 0) {
           count++;
           pos += wordLength;
       }

       return count;
   }

   public void fillTags(Document doc, HashMap<String, Token> tokenMap) {
       for (Token token : tokenMap.values()) {
           token.tags = new ArrayList<>(token.positions.size());
           for (int i = 0; i < token.positions.size(); i++) {
               token.tags.add("body");
           }
       }

       processTagsForField(doc, "title", "title", tokenMap);

       List<String> h1s = doc.getList("h1s", String.class);
       if (h1s != null) {
           for (String h1 : h1s) {
               processTagsForField(doc, h1, "h1", tokenMap);
           }
       }

       List<String> h2s = doc.getList("h2s", String.class);
       if (h2s != null) {
           for (String h2 : h2s) {
               processTagsForField(doc, h2, "h2", tokenMap);
           }
       }

       List<String> h3s = doc.getList("h3s", String.class);
       if (h3s != null) {
           for (String h3 : h3s) {
               processTagsForField(doc, h3, "h3", tokenMap);
           }
       }

       List<String> h456s = doc.getList("h456s", String.class);
       if (h456s != null) {
           for (String h456 : h456s) {
               processTagsForField(doc, h456, "h456", tokenMap);
           }
       }
   }

   private void processTagsForField(Document doc, String text, String tagName, HashMap<String, Token> tokenMap) {
       if (text == null || text.isEmpty()) return;

       String processedText = processHeadingText(text);
       for (Map.Entry<String, Token> entry : tokenMap.entrySet()) {
           String token = entry.getKey();
           int count = countWordOccurrences(processedText, token);
           if (count > 0) {
               Token t = entry.getValue();
               int replaced = 0;
               for (int i = 0; i < t.tags.size() && replaced < count; i++) {
                   if ("body".equals(t.tags.get(i))) {
                       t.tags.set(i, tagName);
                       replaced++;
                   }
               }
           }
       }
   }
}


