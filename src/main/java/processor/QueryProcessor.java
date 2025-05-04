package processor;

import db.DBManager;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.BitSet;

@Component
public class QueryProcessor {
    private final DBManager db;
    private final ExecutorService executor;

    public QueryProcessor(DBManager db) {
        this.db = db;
        this.executor = Executors.newWorkStealingPool();
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
            type = containsBoolean(query) ? "normal+boolean" : "normal";
        }

        Map<String, Map<String, Document>> termDocumentMap;
        if (type.equals("phrase+boolean")) {
            termDocumentMap = handlePhraseWithBoolean(queryWords, query);
        } else if (type.equals("phrase")) {
            termDocumentMap = handlePhrase(queryWords);
        } else {
            termDocumentMap = handleNormal(queryWords);
        }

        Set<String> allDocIds = new HashSet<>();
        for (Map<String, Document> docs : termDocumentMap.values()) {
            allDocIds.addAll(docs.keySet());
        }

        List<String> docIds = new ArrayList<>(allDocIds);
        int totalCount = docIds.size();

        // docDataMap
        // {
        //  "url_1": {
        //    "words": {
        //      "travel": { "tf": 1, "positions": [1], "tags": ["p"] },
        //      "guide": { "tf": 1, "positions": [2], "tags": ["p"] },
        //      "sports": { "tf": 1, "positions": [5], "tags": ["p"] }
        //    }
        //  }
        //}
        Map<String, Map<String, Map<String, Object>>> docDataMap = new ConcurrentHashMap<>();
        termDocumentMap.entrySet().parallelStream().forEach(entry -> {
            String term = entry.getKey();
            entry.getValue().forEach((docId, doc) -> {
                docDataMap.computeIfAbsent(docId, k -> new ConcurrentHashMap<>())
                        .computeIfAbsent("words", k -> new ConcurrentHashMap<>())
                        .put(term, Map.of(
                                "tf", doc.get("tf"),
                                "positions", doc.get("positions"),
                                "tags", doc.get("tags")
                        ));
            });
        });
        // docData
        // [
        //  {
        //    "docId": "url_1",
        //       "words": {
        //          travel": { "tf": 1, "positions": [1], "tags": ["p"] },
        //          "guide": { "tf": 1, "positions": [2], "tags": ["p"] },
        //          "sports": { "tf": 1, "positions": [5], "tags": ["p"] }
        //    }
        //   }
        //]
        List<Map<String, Object>> docData = docIds.stream()
                .map(docId -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("docId", docId);
                    info.put("words", docDataMap.getOrDefault(docId, new ConcurrentHashMap<>()).getOrDefault("words", new HashMap<>()));
                    return info;
                })
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - start;

        QueryResult result = new QueryResult();
        result.setDocIds(docIds);
        System.out.println("docIds: " + docIds);
        result.setDocData(docData);
        result.setType(type);
        result.setTime(duration);
        result.setQueryWords(queryWords);
        result.setQueryWordsString(splitQuery(query));
        result.setTotalCount(totalCount);
        result.setPerWordResults(formatResultForRanker(termDocumentMap));

        return result;
    }

    private boolean containsBoolean(String query) {
        String q = query.toLowerCase();
        return q.contains(" and ") || q.contains(" or ") || q.contains(" not ");
    }

    private Map<String, Map<String, Document>> handleNormal(List<String> terms) {
        List<String> filteredTerms = terms.stream()
                .filter(t -> !t.equalsIgnoreCase("AND") && !t.equalsIgnoreCase("OR") && !t.equalsIgnoreCase("NOT"))
                .toList();

        Map<String, Map<String, Document>> results = db.getDocumentsForWords(filteredTerms);
        return results;
    }

    private Map<String, Map<String, Document>> handlePhrase(List<String> terms) {
        Map<String, Map<String, Document>> termDocs = db.getDocumentsForWords(terms);

        Map<String, Integer> docIdToIndex = new ConcurrentHashMap<>();
        AtomicInteger indexCounter = new AtomicInteger(0);
        termDocs.values().forEach(docs -> docs.keySet().forEach(docId ->
                docIdToIndex.computeIfAbsent(docId, k -> indexCounter.getAndIncrement())));

        BitSet commonDocs = new BitSet();
        boolean first = true;
        for (String term : terms) {
            Map<String, Document> docs = termDocs.getOrDefault(term, new HashMap<>());
            BitSet docSet = new BitSet();
            docs.keySet().forEach(docId -> docSet.set(docIdToIndex.get(docId)));
            if (first) {
                commonDocs.or(docSet);
                first = false;
            } else {
                commonDocs.and(docSet);
            }
        }

        List<String> candidateDocIds = commonDocs.stream()
                .mapToObj(index -> docIdToIndex.entrySet().stream()
                        .filter(entry -> entry.getValue() == index)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<String, Map<String, List<Integer>>> allPositions = db.getPositionsForWordsBatch(terms, candidateDocIds);

        Map<String, Map<String, Document>> phraseResults = new ConcurrentHashMap<>();
        candidateDocIds.parallelStream()
                .filter(docId -> checkPhraseMatch(terms, docId, allPositions))
                .forEach(docId -> {
                    for (String term : terms) {
                        phraseResults.computeIfAbsent(term, k -> new ConcurrentHashMap<>())
                                .put(docId, termDocs.get(term).get(docId));
                    }
                });

        return phraseResults;
    }

    private boolean checkPhraseMatch(List<String> terms, String docId, Map<String, Map<String, List<Integer>>> allPositions) {
        Map<String, List<Integer>> termPositions = new HashMap<>();
        for (String term : terms) {
            Map<String, List<Integer>> positionsForTerm = allPositions.getOrDefault(term, Collections.emptyMap());
            List<Integer> positions = positionsForTerm.get(docId);
            if (positions == null || positions.isEmpty()) {
                return false;
            }
            termPositions.put(term, positions);
        }

        BitSet basePositions = new BitSet();
        termPositions.get(terms.get(0)).forEach(pos -> basePositions.set(pos));

        for (int i = 1; i < terms.size(); i++) {
            BitSet shifted = new BitSet();
            int finalI = i;
            termPositions.get(terms.get(i)).forEach(p -> shifted.set(p - finalI));
            basePositions.and(shifted);
            if (basePositions.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private Map<String, Map<String, Document>> handlePhraseWithBoolean(List<String> queryWords, String query) {
        try {
            List<String> parts = splitQuery(query);
            List<String> operators = extractOperators(parts);
            List<CompletableFuture<Map<String, Map<String, Document>>>> futures = new ArrayList<>();

            for (String part : parts) {
                if (part.equalsIgnoreCase("AND") || part.equalsIgnoreCase("OR") || part.equalsIgnoreCase("NOT")) {
                    continue;
                }
                CompletableFuture<Map<String, Map<String, Document>>> future = CompletableFuture.supplyAsync(() -> {
                    if (part.startsWith("\"") && part.endsWith("\"")) {
                        String phrase = part.substring(1, part.length() - 1);
                        List<String> tokens = Tokenizer.tokenize(phrase).stream().map(Stemmer::stem).toList();
                        return handlePhrase(tokens);
                    } else {
                        String term = Stemmer.stem(part);
                        return Map.of(term, db.getDocumentsForWord(term));
                    }
                }, executor);
                futures.add(future);
            }

            List<Map<String, Map<String, Document>>> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            Map<String, Integer> docIdToIndex = new ConcurrentHashMap<>();
            AtomicInteger indexCounter = new AtomicInteger(0);
            results.forEach(termDocs -> termDocs.values().forEach(docs ->
                    docs.keySet().forEach(docId ->
                            docIdToIndex.computeIfAbsent(docId, k -> indexCounter.getAndIncrement()))));

            List<Set<String>> docIdSets = new ArrayList<>();
            List<Set<String>> termSets = new ArrayList<>();
            for (Map<String, Map<String, Document>> result : results) {
                Set<String> docIds = new HashSet<>();
                result.values().forEach(docs -> docIds.addAll(docs.keySet()));
                docIdSets.add(docIds);
                Set<String> terms = new HashSet<>(result.keySet());
                termSets.add(terms);
            }

            Set<String> mergedDocIds = new HashSet<>(docIdSets.get(0));
            for (int i = 1; i < docIdSets.size(); i++) {
                String op = operators.get(i - 1).toUpperCase();
                Set<String> currentDocIds = docIdSets.get(i);
                mergedDocIds = switch (op) {
                    case "AND" -> { Set<String> result = new HashSet<>(mergedDocIds); result.retainAll(currentDocIds); yield result; }
                    case "OR" -> { Set<String> result = new HashSet<>(mergedDocIds); result.addAll(currentDocIds); yield result; }
                    case "NOT" -> { Set<String> result = new HashSet<>(mergedDocIds); result.removeAll(currentDocIds); yield result; }
                    default -> mergedDocIds;
                };
            }

            Map<String, Map<String, Document>> finalResult = new ConcurrentHashMap<>();
            for (int i = 0; i < results.size(); i++) {
                Map<String, Map<String, Document>> partResult = results.get(i);
                Set<String> partTerms = termSets.get(i);
                for (String term : partTerms) {
                    Map<String, Document> docs = partResult.get(term);
                    Map<String, Document> filteredDocs = new HashMap<>();
                    for (Map.Entry<String, Document> entry : docs.entrySet()) {
                        String docId = entry.getKey();
                        if (mergedDocIds.contains(docId)) {
                            filteredDocs.put(docId, entry.getValue());
                        }
                    }
                    System.out.println("Filtered docs for term " + term + ": " + filteredDocs);
                    if (!filteredDocs.isEmpty()) {
                        finalResult.put(term, filteredDocs);
                    }
                }
            }

            return finalResult;
        } finally {
            // Do not shutdown the executor here; let Spring manage its lifecycle
        }
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
        Map<String, Map<String, Object>> formattedResult = new ConcurrentHashMap<>();
        termDocumentMap.entrySet().parallelStream().forEach(entry -> {
            String term = entry.getKey();
            Map<String, Document> docs = entry.getValue();
            Map<String, Object> docData = new ConcurrentHashMap<>();
            docs.forEach((docId, doc) -> {
                Map<String, Object> docInfo = Map.of(
                        "tf", doc.get("tf"),
                        "positions", doc.get("positions"),
                        "tags", doc.get("tags")
                );
                docData.put(docId, docInfo);
            });
            formattedResult.put(term, docData);
        });
        return formattedResult;
    }
}