package processor;

import db.DBManager;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class QueryProcessor {
    private final DBManager db;

    public QueryProcessor(DBManager db) {
        this.db = db;
    }

    public QueryResult processQuery(String query) {
        long start = System.currentTimeMillis();
        List<String> queryWords;
        String type;

        if (query.contains("\"")) {
            queryWords = extractQueryWordsWithBoolean(query);
            type = containsBoolean(query) ? "phrase+boolean" : "phrase";
        } else {
            queryWords = Tokenizer.tokenize(query).stream().map(Stemmer::stem).toList();
            type = "normal";
        }

        // Process search based on query type
        Map<String, Document> documentData;
        if (query.contains("\"")) {
            documentData = handlePhraseWithBoolean(query);
        } else {
            documentData = handleNormal(queryWords);
        }

        // Convert results to desired format
        List<String> docIds = new ArrayList<>(documentData.keySet());
        List<Map<String, Object>> docData = new ArrayList<>();

        for (String docId : docIds) {
            Document doc = documentData.get(docId);
            Map<String, Object> docInfo = new HashMap<>();

            // Extract data from the document
            docInfo.put("tf", doc.get("tf"));
            docInfo.put("positions", doc.get("positions"));
            docInfo.put("tags", doc.get("tags"));

            docData.add(docInfo);
        }

        long duration = System.currentTimeMillis() - start;

        // Create the final result in the desired format
        QueryResult result = new QueryResult();
        result.setDocIds(docIds);
        result.setDocData(docData);
        result.setType(type);
        result.setTime(duration);
        result.setQueryWords(queryWords);
        result.setQueryWordsString(splitQuery(query));

        return result;
    }

    private boolean containsBoolean(String query) {
        String q = query.toLowerCase();
        return q.contains(" and ") || q.contains(" or ") || q.contains(" not ");
    }

    private Map<String, Document> handlePhraseWithBoolean(String query) {
        List<String> parts = splitQuery(query);
        List<String> operators = extractOperators(parts);
        List<Map<String, Document>> results = new ArrayList<>();

        for (String part : parts) {
            if (part.equalsIgnoreCase("AND") || part.equalsIgnoreCase("OR") || part.equalsIgnoreCase("NOT")) continue;

            if (part.startsWith("\"") && part.endsWith("\"")) {
                String phrase = part.substring(1, part.length() - 1);
                List<String> tokens = Tokenizer.tokenize(phrase).stream().map(Stemmer::stem).toList();
                results.add(phraseSearch(tokens));
            } else {
                String term = Stemmer.stem(part);
                results.add(db.getDocumentsForWord(term));
            }
        }

        Map<String, Document> finalResult = new HashMap<>(results.getFirst());
        for (int i = 1; i < results.size(); i++) {
            String op = operators.get(i - 1);
            Map<String, Document> current = results.get(i);

            switch (op) {
                case "AND" -> finalResult.keySet().retainAll(current.keySet());
                case "OR" -> current.forEach(finalResult::putIfAbsent);
                case "NOT" -> current.keySet().forEach(finalResult::remove);
            }
        }

        return finalResult;
    }

    private Map<String, Document> phraseSearch(List<String> terms) {
        Map<String, Document> result = new HashMap<>();
        List<Map<String, Document>> docLists = terms.stream().map(db::getDocumentsForWord).toList();
        Set<String> commonDocs = new HashSet<>(docLists.get(0).keySet());

        for (int i = 1; i < docLists.size(); i++) {
            commonDocs.retainAll(docLists.get(i).keySet());
        }

        for (String docId : commonDocs) {
            List<Integer> firstPositions = db.getPositionsForWord(terms.get(0), docId);
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
                    // Add the document data to results
                    result.put(docId, docLists.get(0).get(docId));
                    break;
                }
            }
        }

        return result;
    }

    private Map<String, Document> handleNormal(List<String> tokens) {
        Map<String, Document> results = new HashMap<>();
        for (String token : tokens) {
            Map<String, Document> documentMap = db.getDocumentsForWord(token);
            results.putAll(documentMap);
        }
        return results;
    }

    private List<String> splitQuery(String query) {
        return Arrays.stream(query.split("(?= AND | OR | NOT )|(?<= AND | OR | NOT )"))
                .map(String::trim).collect(Collectors.toList());
    }

    private List<String> extractOperators(List<String> parts) {
        return parts.stream()
                .filter(p -> p.equalsIgnoreCase("AND") || p.equalsIgnoreCase("OR") || p.equalsIgnoreCase("NOT"))
                .map(String::toUpperCase)
                .toList();
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