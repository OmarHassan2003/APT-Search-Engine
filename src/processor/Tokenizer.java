package src.processor;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    public static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();

        String lowerInput = input.toLowerCase();

        String cleanedInput = lowerInput.replaceAll("[^a-zA-Z0-9\\s]", " ").trim();

        String[] words = cleanedInput.split("\\s+");

        for (String word : words) {
            if (!word.isEmpty()) {
                tokens.add(word);
            }
        }

        return tokens;
    }
}