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
        // Use a dynamic thread pool based on available processors
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

        int fromIndex = Math.min(page * size, docIds.size());
        int toIndex = Math.min(fromIndex + size, docIds.size());
        List<String> paginatedDocIds = docIds.subList(fromIndex, toIndex);

        Map<String, Map<String, Document>> paginatedTermDocumentMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Document>> entry : termDocumentMap.entrySet()) {
            String term = entry.getKey();
            Map<String, Document> filteredDocs = entry.getValue().entrySet().stream()
                    .filter(e -> paginatedDocIds.contains(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            paginatedTermDocumentMap.put(term, filteredDocs);
        }

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
        result.setPerWordResults(formatResultForRanker(paginatedTermDocumentMap));

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
        // Batch fetch documents for all terms
        Map<String, Map<String, Document>> termDocs = db.getDocumentsForWords(terms);

        // Map docIds to unique indices to avoid hash collisions
        Map<String, Integer> docIdToIndex = new ConcurrentHashMap<>();
        AtomicInteger indexCounter = new AtomicInteger(0);
        termDocs.values().forEach(docs -> docs.keySet().forEach(docId ->
                docIdToIndex.computeIfAbsent(docId, k -> indexCounter.getAndIncrement())));

        // Find common documents using BitSet
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


        // Batch fetch positions for all common documents
        List<String> candidateDocIds = commonDocs.stream()
                .mapToObj(index -> docIdToIndex.entrySet().stream()
                        .filter(entry -> entry.getValue() == index)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // Assuming DBManager supports batch fetching positions for multiple docIds
        Map<String, Map<String, List<Integer>>> allPositions = db.getPositionsForWordsBatch(terms, candidateDocIds);

        // Filter for phrase matches
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
        // Fetch positions from the pre-fetched map
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

            // Process each query part asynchronously
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

            // Wait for all parts to complete
            List<Map<String, Map<String, Document>>> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            // Map docIds to unique indices for all documents in results
            Map<String, Integer> docIdToIndex = new ConcurrentHashMap<>();
            AtomicInteger indexCounter = new AtomicInteger(0);
            results.forEach(termDocs -> termDocs.values().forEach(docs ->
                    docs.keySet().forEach(docId ->
                            docIdToIndex.computeIfAbsent(docId, k -> indexCounter.getAndIncrement()))));

            // Apply boolean operations
            Map<String, Map<String, Document>> merged = new ConcurrentHashMap<>(results.get(0));
            for (int i = 1; i < results.size(); i++) {
                String op = operators.get(i - 1).toUpperCase();
                Map<String, Map<String, Document>> current = results.get(i);

                Set<String> allTerms = new HashSet<>();
                allTerms.addAll(merged.keySet());
                allTerms.addAll(current.keySet());

                Map<String, Map<String, Document>> newMerged = new ConcurrentHashMap<>();
                Map<String, Map<String, Document>> finalMerged = merged;
                allTerms.parallelStream().forEach(term -> {
                    Map<String, Document> left = finalMerged.getOrDefault(term, new HashMap<>());
                    Map<String, Document> right = current.getOrDefault(term, new HashMap<>());

                    Map<String, Document> mergedDocs = switch (op) {
                        case "AND" -> {
                            Map<String, Document> out = new HashMap<>();
                            BitSet leftDocs = new BitSet();
                            left.keySet().forEach(docId -> leftDocs.set(docIdToIndex.get(docId)));
                            for (String docId : right.keySet()) {
                                if (leftDocs.get(docIdToIndex.get(docId))) {
                                    out.put(docId, left.get(docId));
                                }
                            }
                            yield out;
                        }
                        case "OR" -> {
                            Map<String, Document> out = new HashMap<>();
                            BitSet allDocs = new BitSet();
                            left.keySet().forEach(docId -> allDocs.set(docIdToIndex.get(docId)));
                            right.keySet().forEach(docId -> allDocs.set(docIdToIndex.get(docId)));
                            docIdToIndex.forEach((docId, index) -> {
                                if (allDocs.get(index)) {
                                    Document doc = left.getOrDefault(docId, right.get(docId));
                                    if (doc != null) {
                                        out.put(docId, doc);
                                    }
                                }
                            });
                            yield out;
                        }
                        case "NOT" -> {
                            Map<String, Document> out = new HashMap<>();
                            BitSet leftDocs = new BitSet();
                            BitSet rightDocs = new BitSet();
                            left.keySet().forEach(docId -> leftDocs.set(docIdToIndex.get(docId)));
                            right.keySet().forEach(docId -> rightDocs.set(docIdToIndex.get(docId)));
                            leftDocs.andNot(rightDocs);
                            docIdToIndex.forEach((docId, index) -> {
                                if (leftDocs.get(index)) {
                                    Document doc = left.get(docId);
                                    if (doc != null) {
                                        out.put(docId, doc);
                                    }
                                }
                            });
                            yield out;
                        }
                        default -> new HashMap<>();
                    };

                    if (!mergedDocs.isEmpty()) {
                        newMerged.put(term, mergedDocs);
                    }
                });

                merged = newMerged;
            }

            return merged;
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