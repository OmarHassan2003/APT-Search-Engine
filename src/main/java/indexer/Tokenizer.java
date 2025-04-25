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
    public List<Integer> positions;

    public Token(String word) {
        this.word = word;
        this.count = 1;
        this.positions = new ArrayList<>();
    }

    public void increment() {
        this.count++;
    }

    public void addPosition(int pos) {
        this.positions.add(pos);
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

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.isBlank()) continue;
    
            if (tokenMap.containsKey(token)) {
                tokenMap.get(token).increment();
                tokenMap.get(token).addPosition(i);
            } else {
                Token t = new Token(token);
                t.addPosition(i);
                tokenMap.put(token, t);
            }
        }


        // turn token.count --> token.TF ( token occurance / total tokens )

        for (String token : tokenMap.keySet()) {
            tokenMap.get(token).count = tokenMap.get(token).count / tokens.size();
        }

        /*
            For Ranker:

            Discuss with Document Model creator
            how to define the position of the document
        
        */

        return tokenMap;
    }

    public List<String> tokenizeString(String text) {
        List<String> tokens = new ArrayList<String>();
        
        String currentText = text.toLowerCase().replaceAll("[^a-z]", "");
        String[] words = currentText.split("//s+");

        for (String word : words) {
            
            /*
                TODO: 
                    Stemming should be here for query searching 
                    (Tony want to merge)
            */

            tokens.add(word);
        }

        return tokens;
    }

}


