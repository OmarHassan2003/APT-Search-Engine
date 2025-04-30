package processor;

import db.DBManager;

import java.util.*;
import java.util.stream.Collectors;



public class QueryProcessor {
    private final DBManager db;
    public QueryProcessor(DBManager db) {
        this.db = db;
    }

    public QueryResult processQuery(String query) {
        long start = System.currentTimeMillis();
        Set<String> result;
        String type;
        List<String> queryWords;

        if (query.contains("\"")) {
            queryWords = extractQueryWordsWithBoolean(query);
            result = handlePhraseWithBoolean(query);
            type = containsBoolean(query) ? "phrase+boolean" : "phrase";
        } else {
            queryWords = Tokenizer.tokenize(query).stream().map(Stemmer::stem).toList();
            result = handleNormal(queryWords);
            type = "normal";
        }

        long duration = System.currentTimeMillis() - start;
        return new QueryResult(result, type, duration, queryWords);
    }

    private boolean containsBoolean(String query) {
        String q = query.toLowerCase();
        return q.contains(" and ") || q.contains(" or ") || q.contains(" not ");
    }

    private Set<String> handlePhraseWithBoolean(String query) {
        List<String> parts = splitQuery(query);
        List<String> operators = extractOperators(parts);
        List<Set<String>> results = new ArrayList<>();

        for (String part : parts) {
            if (part.equalsIgnoreCase("AND") || part.equalsIgnoreCase("OR") || part.equalsIgnoreCase("NOT")) continue;

            if (part.startsWith("\"") && part.endsWith("\"")) {
                String phrase = part.substring(1, part.length() - 1);
                List<String> tokens = Tokenizer.tokenize(phrase).stream().map(Stemmer::stem).toList();
                results.add(phraseSearch(tokens));
            } else {
                String term = Stemmer.stem(part);
                results.add(new HashSet<>(db.getDocumentsForWord(term)));
            }
        }

        Set<String> finalSet = results.getFirst();
        for (int i = 1; i < results.size(); i++) {
            String op = operators.get(i - 1);
            Set<String> current = results.get(i);
            switch (op) {
                case "AND" -> finalSet.retainAll(current);
                case "OR" -> finalSet.addAll(current);
                case "NOT" -> finalSet.removeAll(current);
            }
        }

        return finalSet;
    }

    private Set<String> handleNormal(List<String> tokens) {
        //List<String> tokens = Tokenizer.tokenize(query).stream().map(Stemmer::stem).toList();
        Set<String> results = null;
        for (String token : tokens) {
            Set<String> docs = new HashSet<>(db.getDocumentsForWord(token));
            if (results == null) results = docs;
            else results.addAll(docs);
        }
        return results != null ? results : new HashSet<>();
    }

    private List<String> splitQuery(String query) {
        return Arrays.stream(query.split("(?= AND | OR | NOT )|(?<= AND | OR | NOT )"))
                .map(String::trim).collect(Collectors.toList());
    }

    private List<String> extractOperators(List<String> parts) {
        List<String> filteredOperators = parts.stream()
                .filter(p -> p.equalsIgnoreCase("AND") || p.equalsIgnoreCase("OR") || p.equalsIgnoreCase("NOT"))
                .toList();

        return filteredOperators.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    private Set<String> phraseSearch(List<String> terms) {
        Set<String> result = new HashSet<>();
        List<List<String>> docLists = terms.stream().map(db::getDocumentsForWord).toList();
        Set<String> commonDocs = new HashSet<>(docLists.getFirst());
        for (int i = 1; i < docLists.size(); i++) {
            commonDocs.retainAll(docLists.get(i));
        }


        for (String docId : commonDocs) {
            List<Integer> firstPositions = db.getPositionsForWord(terms.getFirst(), docId);
            for (int pos : firstPositions) {
                boolean match = true;
                for (int i = 1; i < terms.size(); i++) {
                    List<Integer> positions = db.getPositionsForWord(terms.get(i), docId);
                    if (!positions.contains(pos + i)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    result.add(docId);
                    break;
                }
            }
        }

        return result;
    }

    private List<String> extractQueryWordsWithBoolean(String query) {
        List<String> parts = splitQuery(query);
        List<String> queryWords = new ArrayList<>();
        for (String part : parts) {
            if (part.equalsIgnoreCase("AND") || part.equalsIgnoreCase("OR") || part.equalsIgnoreCase("NOT")) continue;
            if (part.startsWith("\"") && part.endsWith("\"")) {
                String phrase = part.substring(1, part.length() - 1);
                queryWords.addAll(Tokenizer.tokenize(phrase).stream().map(Stemmer::stem).toList());
            } else {
                queryWords.add(Stemmer.stem(part));
            }
        }
        return queryWords;
    }

}


