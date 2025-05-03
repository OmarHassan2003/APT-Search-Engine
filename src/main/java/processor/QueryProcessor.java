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

    public QueryResult processQuery(String query, int page, int size) {
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

        Map<String, Map<String, Document>> termDocumentMap;
        if (type.equals("phrase+boolean")) {
            termDocumentMap = handlePhraseWithBoolean(queryWords, query);
        } else if (type.equals("phrase")) {
            termDocumentMap = handlePhrase(queryWords);
        } else {
            termDocumentMap = handleNormal(queryWords);
        }
        //System.out.println("termDocumentMap: " + termDocumentMap);
        Set<String> allDocIds = new HashSet<>();
        for (Map<String, Document> docs : termDocumentMap.values()) {
            allDocIds.addAll(docs.keySet());
        }
        //System.out.println("allDocIds: " + allDocIds);

        List<String> docIds = new ArrayList<>(allDocIds);
        int totalCount = docIds.size();

        int fromIndex = Math.min(page * size, docIds.size());
        int toIndex = Math.min(fromIndex + size, docIds.size());
        List<String> paginatedDocIds = docIds.subList(fromIndex, toIndex);
        System.out.println("paginatedDocIds: " + paginatedDocIds);

        // Filter termDocumentMap to include only paginatedDocIds
        Map<String, Map<String, Document>> paginatedTermDocumentMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Document>> entry : termDocumentMap.entrySet()) {
            String term = entry.getKey();
            Map<String, Document> filteredDocs = entry.getValue().entrySet().stream()
                    .filter(e -> paginatedDocIds.contains(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            paginatedTermDocumentMap.put(term, filteredDocs);
        }
        System.out.println("paginatedTermDocumentMap: " + paginatedTermDocumentMap);

        // Build docData for paginated documents
        List<Map<String, Object>> docData = new ArrayList<>();
        for (String docId : paginatedDocIds) {
            Map<String, Object> info = new HashMap<>();
            info.put("docId", docId);
            Map<String, Object> wordInfo = new HashMap<>();

            for (String term : queryWords) {
                Document doc = paginatedTermDocumentMap.getOrDefault(term, Collections.emptyMap()).get(docId);
                if (doc != null) {
                    wordInfo.put(term, Map.of(
                            "tf", doc.get("tf"),
                            "positions", doc.get("positions"),
                            "tags", doc.get("tags")
                    ));
                }
            }
            info.put("words", wordInfo);
            docData.add(info);
        }

        long duration = System.currentTimeMillis() - start;

        QueryResult result = new QueryResult();
        result.setDocIds(paginatedDocIds);
        result.setDocData(docData);
        result.setType(type);
        result.setTime(duration);
        result.setQueryWords(queryWords);
        result.setQueryWordsString(splitQuery(query));
        result.setTotalCount(totalCount);
        result.setPerWordResults(formatResultForRanker(paginatedTermDocumentMap)); // Adjusted method call

        System.out.println("result: " + result);
        return result;
    }

    private boolean containsBoolean(String query) {
        String q = query.toLowerCase();
        return q.contains(" and ") || q.contains(" or ") || q.contains(" not ");
    }

    private Map<String, Map<String, Document>> handleNormal(List<String> terms) {
        Map<String, Map<String, Document>> results = new HashMap<>();
        for (String term : terms) {
            results.put(term, db.getDocumentsForWord(term));
        }
        return results;
    }

    private Map<String, Map<String, Document>> handlePhrase(List<String> terms) {
        Map<String, Map<String, Document>> termDocs = handleNormal(terms);

        Set<String> commonDocs = new HashSet<>(termDocs.get(terms.get(0)).keySet());
        for (int i = 1; i < terms.size(); i++) {
            commonDocs.retainAll(termDocs.get(terms.get(i)).keySet());
        }

        Set<String> matchedDocs = new HashSet<>();

        for (String docId : commonDocs) {
            boolean matched = checkPhraseMatch(terms, docId);
            if (matched) matchedDocs.add(docId);
        }

        Map<String, Map<String, Document>> phraseResults = new HashMap<>();
        for (String term : terms) {
            Map<String, Document> filtered = new HashMap<>();
            for (String docId : matchedDocs) {
                filtered.put(docId, termDocs.get(term).get(docId));
            }
            phraseResults.put(term, filtered);
        }

        return phraseResults;
    }

    private boolean checkPhraseMatch(List<String> terms, String docId) {
        List<List<Integer>> positions = new ArrayList<>();
        for (String term : terms) {
            List<Integer> pos = db.getPositionsForWord(term, docId);
            if (pos == null) return false;
            positions.add(pos);
        }

        Set<Integer> basePositions = new HashSet<>(positions.get(0));
        for (int i = 1; i < positions.size(); i++) {
            int finalI = i;
            Set<Integer> shifted = positions.get(i).stream()
                    .map(p -> p - finalI).collect(Collectors.toSet());
            basePositions.retainAll(shifted);
            if (basePositions.isEmpty()) return false;
        }

        return true;
    }

    private Map<String, Map<String, Document>> handlePhraseWithBoolean(List<String> queryWords, String query) {
        List<String> parts = splitQuery(query);
        List<String> operators = extractOperators(parts);
        List<Map<String, Map<String, Document>>> results = new ArrayList<>();

        for (String part : parts) {
            if (part.equalsIgnoreCase("AND") || part.equalsIgnoreCase("OR") || part.equalsIgnoreCase("NOT")) continue;

            if (part.startsWith("\"") && part.endsWith("\"")) {
                String phrase = part.substring(1, part.length() - 1);
                List<String> tokens = Tokenizer.tokenize(phrase).stream().map(Stemmer::stem).toList();
                results.add(handlePhrase(tokens));
            } else {
                String term = Stemmer.stem(part);
                results.add(Map.of(term, db.getDocumentsForWord(term)));
            }
        }

        Map<String, Map<String, Document>> merged = new HashMap<>(results.get(0));

        for (int i = 1; i < results.size(); i++) {
            String op = operators.get(i - 1);
            Map<String, Map<String, Document>> current = results.get(i);

            Set<String> allTerms = new HashSet<>();
            allTerms.addAll(merged.keySet());
            allTerms.addAll(current.keySet());

            Map<String, Map<String, Document>> newMerged = new HashMap<>();

            for (String term : allTerms) {
                Map<String, Document> left = merged.getOrDefault(term, new HashMap<>());
                Map<String, Document> right = current.getOrDefault(term, new HashMap<>());

                Map<String, Document> mergedDocs = switch (op) {
                    case "AND" -> {
                        Set<String> common = new HashSet<>(left.keySet());
                        common.retainAll(right.keySet());
                        Map<String, Document> out = new HashMap<>();
                        for (String docId : common) out.put(docId, left.get(docId));
                        yield out;
                    }
                    case "OR" -> {
                        Map<String, Document> out = new HashMap<>(left);
                        right.forEach(out::putIfAbsent);
                        yield out;
                    }
                    case "NOT" -> {
                        Map<String, Document> out = new HashMap<>(left);
                        right.keySet().forEach(out::remove);
                        yield out;
                    }
                    default -> new HashMap<>();
                };

                if (!mergedDocs.isEmpty()) newMerged.put(term, mergedDocs);
            }

            merged = newMerged;
        }

        return merged;
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

    private Map<String, Map<String, Object>> formatResultForRanker(Map<String, Map<String, Document>> termDocumentMap) {
        Map<String, Map<String, Object>> formattedResult = new HashMap<>();

        for (Map.Entry<String, Map<String, Document>> entry : termDocumentMap.entrySet()) {
            String term = entry.getKey();
            Map<String, Document> docs = entry.getValue();

            Map<String, Object> docData = new HashMap<>();
            for (Map.Entry<String, Document> docEntry : docs.entrySet()) {
                String docId = docEntry.getKey();
                Document doc = docEntry.getValue();

                Map<String, Object> docInfo = new HashMap<>();
                docInfo.put("tf", doc.get("tf"));
                docInfo.put("positions", doc.get("positions"));
                docInfo.put("tags", doc.get("tags"));

                docData.put(docId, docInfo);
            }

            formattedResult.put(term, docData);
        }

        return formattedResult;
    }
}
