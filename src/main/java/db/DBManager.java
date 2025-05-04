package db;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;

import Indexer.Tokenizer.Token;

import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Indexes;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class DBManager {
    private final MongoClient mongoClient;
    private final MongoCollection<Document> docCollection;
    private final MongoCollection<Document> indexCollection;
    private final MongoDatabase database;

    // Optimized bulk write batch size for balance of performance and memory usage
    private static final int BULK_WRITE_BATCH_SIZE = 500;

    // Connection pool sized for optimal throughput
    private static final int MAX_CONNECTIONS = 100;


    public DBManager() {

        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString("mongodb://localhost:27017"))
            .applyToConnectionPoolSettings(builder ->
                builder.maxSize(MAX_CONNECTIONS)
                      .minSize(20) // Minimum size for connection pool
                      .maxWaitTime(2000, TimeUnit.MILLISECONDS) // Reduced wait time
                      .maxConnectionIdleTime(30000, TimeUnit.MILLISECONDS))
            .applyToSocketSettings(builder ->
                builder.connectTimeout(5000, TimeUnit.MILLISECONDS) // Reduced connect timeout
                      .readTimeout(30000, TimeUnit.MILLISECONDS))
            .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase("searchengine");

        this.docCollection = database.getCollection("Crawled_Documents");
        this.indexCollection = database.getCollection("inverted_index");

        // Create indexes if they don't exist (only needed once)
        createIndexes();

    }

    // Create MongoDB indexes to speed up queries.
    private void createIndexes() {
        try {
            boolean hasIsIndexedIndex = false;
            for (Document index : docCollection.listIndexes()) {
                if (index.get("name", "").toString().contains("isIndexed_1")) {
                    hasIsIndexedIndex = true;
                    break;
                }
            }

            if (!hasIsIndexedIndex) {
                docCollection.createIndex(Indexes.ascending("isIndexed"));
                System.out.println("[INFO] Created index on isIndexed field");
            }

            boolean hasTermIndex = false;
            for (Document index : indexCollection.listIndexes()) {
                if (index.get("name", "").toString().contains("term_1")) {
                    hasTermIndex = true;
                    break;
                }
            }

            if (!hasTermIndex) {
                indexCollection.createIndex(Indexes.ascending("term"));
                System.out.println("[INFO] Created index on term field in inverted index");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Error creating indexes: " + e.getMessage());
        }
    }


    public void close() {
        mongoClient.close(); // Properly close the MongoClient
    }

    public void insertDoc(Document doc) {
        try {
            // Insert the document into the collection
            docCollection.insertOne(doc);
        } catch (Exception e) {
            System.out.println("Error inserting document: " + e.getMessage());
        }
    }

    // Bulk insert crawled documents
    public void insertDocuments(List<Document> documents) {
        try {
            docCollection.insertMany(documents);
        } catch (Exception e) {
            System.err.println("Error inserting documents: " + e.getMessage());
        }
    }

    // Fetch a document by URL
    public Document getDocumentByUrl(String url) {
        return docCollection.find(Filters.eq("url", url)).first();
    }

    // Fetch a document by ID
    public Document getDocumentById(String id) {
        return docCollection.find(Filters.eq("_id", new ObjectId(id))).first();
    }

    // Optimize fetching unindexed documents with projection and streaming
    public List<Document> getUnIndexedDocs(int limit) {
        List<Document> docs = new ArrayList<>();

        // Minimize fields retrieved to reduce memory usage
        Document projection = new Document("_id", 1)
                                .append("title", 1)
                                .append("body", 1)
                                .append("h1s", 1)
                                .append("h2s", 1)
                                .append("h3s", 1)
                                .append("h456s", 1);

        // Use a batch size to limit the number of documents fetched at once
        int batchSize = Math.min(50, limit);
        try (MongoCursor<Document> cursor = docCollection.find(Filters.eq("isIndexed", false))
                                        .projection(projection)
                                        .batchSize(batchSize)
                                        .limit(limit)
                                        .hint(Indexes.ascending("isIndexed"))
                                        .iterator()) {

            int count = 0;
            while (cursor.hasNext()) {
                docs.add(cursor.next());
                count++;

                // Check memory usage periodically and clean if necessary
                if (count % 20 == 0 && isMemoryUsageHigh()) {
                    System.out.println("[DEBUG] Memory usage high during document fetch - suggesting cleanup");
                    System.gc();
                }
            }
        } // Cursor automatically closes when leaving this block

        return docs;
    }

    public void insertInverted(String docId, HashMap<String, Token> tokens) {
        System.out.println("[DEBUG] insertInverted called for docId: " + docId + " with " + tokens.size() + " tokens.");
        if (tokens.isEmpty()) {
            System.out.println("[DEBUG] No tokens to insert for docId: " + docId);
            markDocumentAsIndexed(docId);
            return;
        }


        Document doc = null;
        String docTitle = null;
        try {
            doc = docCollection.find(Filters.eq("_id", new ObjectId(docId)))
                               .projection(new Document("title", 1))
                               .first();
            docTitle = doc != null ? doc.getString("title") : "Unknown Title";
        } finally {
            // Clear reference to help garbage collection
            doc = null;
        }

        List<WriteModel<Document>> updates = new ArrayList<>(BULK_WRITE_BATCH_SIZE);
        int batchCounter = 0;


        List<String> termKeys = new ArrayList<>(tokens.keySet());

        for (String term : termKeys) {
            Token token = tokens.get(term);
            if (token == null || token.positions.isEmpty()) {
                // Remove the entry from the map immediately to free memory
                tokens.remove(term);
                continue;
            }

            Document docPosting = new Document("tf", token.count)
                    .append("positions", token.positions)
                    .append("tags", token.tags)
                    .append("title", docTitle);

            Document setFields = new Document("updated_at", new Date())
                    .append("postings." + docId, docPosting);

            UpdateOneModel<Document> updateModel = new UpdateOneModel<>(
                    Filters.eq("term", term),
                    new Document()
                            .append("$setOnInsert", new Document("term", term))
                            .append("$set", setFields),
                    new UpdateOptions().upsert(true)
            );

            updates.add(updateModel);

            // Remove processed entry immediately to free memory
            tokens.remove(term);

            // Execute batch if we've reached the batch size threshold
            batchCounter++;
            if (batchCounter >= BULK_WRITE_BATCH_SIZE || isMemoryUsageHigh()) {
                indexCollection.bulkWrite(updates);
                System.out.println("[DEBUG] Executed bulk write batch of " + updates.size() + " operations");

                updates.clear();
                batchCounter = 0;
                System.gc();

                // Add small pause to allow GC to work
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Execute any remaining updates
        if (!updates.isEmpty()) {
            indexCollection.bulkWrite(updates);
            System.out.println("[DEBUG] Tokens successfully inserted into inverted_index. Final batch size: " + updates.size());
            updates.clear();
        }

        markDocumentAsIndexed(docId);

        // Clear all references to help garbage collection
        tokens.clear();
        termKeys.clear();
        System.gc();
    }

    private boolean isMemoryUsageHigh() {

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();

        double usedPercentage = (double)usedMemory / maxMemory * 100.0;

        if (usedPercentage > 70.0) {  // Using a simple 70% threshold
            System.out.println("[DEBUG] Memory usage high: " + usedPercentage + "%");
            return true;
        }
        return false;
    }

    public void markDocumentAsIndexed(String docId) {
        docCollection.updateOne(
            Filters.eq("_id", new ObjectId(docId)),
            Updates.set("isIndexed", true)
        );
        System.out.println("[DEBUG] Document marked as indexed without processing: " + docId);
    }

//    public List<String> getDocumentsForWord(String word) {
//        List<String> docIds = new ArrayList<>();
//        Document query = new Document("term", word);
//        Document result = indexCollection.find(query).first();
//        if (result != null) {
//            Document postings = result.get("postings", Document.class);
//            if (postings != null) {
//                docIds.addAll(postings.keySet());
//            }
//        }
//        return docIds;
//    }

//    public Map<String, Document> getDocumentsForWord(String word) {
//        Map<String, Document> documentsWithData = new HashMap<>();
//
//        // Create a query to find the term in the inverted index
//        Document query = new Document("term", word);
//
//        // Find the document for this term
//        Document result = indexCollection.find(query).first();
//
//        if (result != null) {
//            // Get the postings document that contains all documents where this term appears
//            Document postings = result.get("postings", Document.class);
//
//            if (postings != null) {
//                // For each document ID in the postings
//                for (String docId : postings.keySet()) {
//                    // Get the document data (which includes tf, positions, etc.)
//                    Document docData = postings.get(docId, Document.class);
//
//                    // Add this document and its data to our result map
//                    documentsWithData.put(docId, docData);
//                }
//            }
//        }
//
//        return documentsWithData;
//    }
    public Map<String, Document> getDocumentsForWord(String word) {
        Map<String, Document> documentsWithMetadata = new HashMap<>();

        // Create a query to find the term in the inverted index
        Document query = new Document("term", word);

        // Find the document for this term
        Document result = indexCollection.find(query).first();
        if (result != null) {
            // Get the postings document that contains all documents where this term appears
            Document postings = result.get("postings", Document.class);
            if (postings != null) {
                // For each document ID in the postings
                for (String docId : postings.keySet()) {
                    // Get the document data (including term frequency, positions, etc.)
                    Document docData = postings.get(docId, Document.class);

                    // Create a new document to hold the metadata (TF, positions, etc.)
                    Document documentWithMetadata = new Document("tf", docData.get("tf"))
                            .append("positions", docData.get("positions"))
                            .append("tags", docData.get("tags"))
                            .append("title", docData.get("title"));

                    // Add this document with metadata to the result map
                    documentsWithMetadata.put(docId, documentWithMetadata);
                }
            }
        }

        return documentsWithMetadata;
    }


    public List<Integer> getPositionsForWord(String word, String docId) {
        List<Integer> positions = new ArrayList<>();
        Document query = new Document("term", word);
        Document result = indexCollection.find(query).first();
        if (result != null) {
            Document postings = result.get("postings", Document.class);
            if (postings != null && postings.containsKey(docId)) {
                Document docData = postings.get(docId, Document.class);
                positions = (List<Integer>) docData.get("positions");
            }
        }
        return positions;
    }

    public Map<String, Map<String, Document>> getDocumentsForWords(List<String> words) {
        Map<String, Map<String, Document>> termDocs = new ConcurrentHashMap<>();
        FindIterable<Document> results = indexCollection.find(Filters.in("term", words));
        for (Document result : results) {
            String term = result.getString("term");
            Document postings = result.get("postings", Document.class);
            if (postings != null) {
                Map<String, Document> docs = new ConcurrentHashMap<>();
                for (String docId : postings.keySet()) {
                    Document docData = postings.get(docId, Document.class);
                    docs.put(docId, new Document("tf", docData.get("tf"))
                            .append("positions", docData.get("positions"))
                            .append("tags", docData.get("tags"))
                            .append("title", docData.get("title")));
                }
                termDocs.put(term, docs);
            }
        }
        return termDocs;
    }

    public Map<String, List<Integer>> getPositionsForWords(List<String> words, String docId) {
        Map<String, List<Integer>> termPositions = new ConcurrentHashMap<>();
        FindIterable<Document> results = indexCollection.find(Filters.in("term", words));
        for (Document result : results) {
            String term = result.getString("term");
            Document postings = result.get("postings", Document.class);
            if (postings != null && postings.containsKey(docId)) {
                Document docData = postings.get(docId, Document.class);
                termPositions.put(term, (List<Integer>) docData.get("positions"));
            }
        }
        return termPositions;
    }

    public Map<String, Map<String, List<Integer>>> getPositionsForWordsBatch(List<String> terms, List<String> candidateDocIds) {
        // Handle empty input
        if (terms.isEmpty() || candidateDocIds.isEmpty()) {
            return new HashMap<>();
        }

        // Build aggregation pipeline
        List<Document> pipeline = new ArrayList<>();

        // Stage 1: Match documents where term is in the terms list
        Document matchStage = new Document("$match", new Document("term", new Document("$in", terms)));
        pipeline.add(matchStage);

        // Stage 2: Convert postings object to array of { k: docId, v: { tf, positions, tags, title } }
        Document objectToArrayStage = new Document("$addFields", new Document("postingsArray",
                new Document("$objectToArray", "$postings")
        ));
        pipeline.add(objectToArrayStage);

        // Stage 3: Unwind the postingsArray
        Document unwindStage = new Document("$unwind", "$postingsArray");
        pipeline.add(unwindStage);

        // Stage 4: Match postings where docId (postingsArray.k) is in candidateDocIds
        Document matchPostingsStage = new Document("$match", new Document("postingsArray.k", new Document("$in", candidateDocIds)));
        pipeline.add(matchPostingsStage);

        // Stage 5: Project to reshape the output
        Document projectStage = new Document("$project", new Document()
                .append("term", 1)
                .append("docId", "$postingsArray.k")
                .append("positions", "$postingsArray.v.positions")
                .append("_id", 0));
        pipeline.add(projectStage);

        // Execute aggregation
        AggregateIterable<Document> results = indexCollection.aggregate(pipeline);

        // Process results into the required map structure
        Map<String, Map<String, List<Integer>>> termDocPositions = new HashMap<>();
        for (Document doc : results) {
            String term = doc.getString("term");
            String docId = doc.getString("docId");
            List<Integer> positions = doc.getList("positions", Integer.class);

            termDocPositions.computeIfAbsent(term, k -> new HashMap<>())
                    .put(docId, positions);
        }

        System.out.println("getPositionsForWordsBatch: Terms " + terms + ", CandidateDocIds " + candidateDocIds);
        System.out.println("getPositionsForWordsBatch: Results " + termDocPositions);
        return termDocPositions;
    }

    public Map<String, Double> getPageRank() {
        Map<String, Double> pageRankMap = new HashMap<>();

        MongoCollection<Document> collection = database.getCollection("pageRanks");

        FindIterable<Document> documents = collection.find();
        for (Document doc : documents) {
            String url = doc.getString("url");
            Double rank = doc.getDouble("rank");

            if (url != null && rank != null) {
                pageRankMap.put(url, rank);
            }
        }

        return pageRankMap;
    }

}

