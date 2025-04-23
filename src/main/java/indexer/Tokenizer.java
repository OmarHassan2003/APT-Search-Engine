package indexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Tokenizer {

    public class Token {
        public String word;
        public double count;
        public String position;

        public Token(String word) {
            this.word = word;
            this.count = 1;
            this.position = "other";
        }

        public void increment() {
            this.count++;
        } 
    }

    private HashSet<String> stopWords;

    public Tokenizer(String filename) {
        try {
            BufferedReader reader = new BufferedReader("../data/stopwords.txt");
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line.trim());
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public HashMap<String, Token> tokenizeDoc(Document doc) {
        String text = doc.text();

        
        // cut the document into list of strings
        
        List<String> tokens = tokenizeString(text);
        HashMap<String, Token> tokenMap = new HashMap<String, Token>();
        

        // calculate the occurance of each token

        for(String token : tokens)  {
            if (tokenMap.containsKey(token)) {
                tokenMap.get(token).increment();
            } else {
                tokenMap.put(token, new Token(token));
            }
        }


        // turn token.count --> token.TF ( token occurance / total tokens )

        for (String token : tokenMap.keySet()) {
            tokenMap.get(token).count = tokenMap.get(token).count / tokens.size();
        }

        /*
            Discuss with Document Model creator
            how to define the position of the document
        */
    }

    public List<String> tokenizeString(String text) {
        List<String> tokens = new ArrayList<String>();
        
        String currentText = text.toLowerCase().replaceAll("[^a-z]", "");
        String[] words = currentText.split("//s+");

        for (String word : words) {
            
            // TODO: Stemming should be here

            tokens.add(word);
        }

        return tokens;
    }

}


