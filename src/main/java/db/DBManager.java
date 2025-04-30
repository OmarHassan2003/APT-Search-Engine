package db;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;

import Indexer.Tokenizer.Token;

import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

public class DBManager {
    private final MongoClient mongoClient;
    private final MongoCollection<Document> docCollection;
    private final MongoCollection<Document> indexCollection;

    public DBManager() {
        String connectionString = "mongodb://localhost:27017";
        this.mongoClient = MongoClients.create(connectionString);
        MongoDatabase database = mongoClient.getDatabase("searchengine");

        this.docCollection = database.getCollection("Documents");
        this.indexCollection = database.getCollection("inverted_index");
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

    // Fetch unindexed Documents
    public List<Document> getUnIndexedDocs(int limit) {
        System.out.println("[DEBUG] Fetching unindexed documents...");
        List<Document> docs = new ArrayList<>();
        FindIterable<Document> results = docCollection.find(Filters.eq("isIndexed", false)).limit(limit);

        for (Document doc : results) {
            System.out.println("[DEBUG] Unindexed document: " + doc.getObjectId("_id"));
            docs.add(doc);
        }

        System.out.println("[DEBUG] Found " + docs.size() + " unindexed documents.");
        return docs;
    }

    // Insert tokens into the inverted index
    public void insertInverted(String docId, HashMap<String, Token> tokens) {
        System.out.println("[DEBUG] insertInverted called for docId: " + docId + " with " + tokens.size() + " tokens.");
        if (tokens.isEmpty()) {
            System.out.println("[DEBUG] No tokens to insert for docId: " + docId);
            return;
        }

        // Fetch the document's title for reference only
        Document doc = getDocumentById(docId);
        String docTitle = doc != null ? doc.getString("title") : "Unknown Title";

        List<WriteModel<Document>> updates = new ArrayList<>();

        for (Map.Entry<String, Token> entry : tokens.entrySet()) {
            String term = entry.getKey();
            Token token = entry.getValue();

            // Ensure positions are populated
            if (token.positions.isEmpty()) {
                System.out.println("[DEBUG] No positions for term: " + term + " in document: " + docTitle);
                continue;
            }

            Document docPosting = new Document("tf", token.count)
                    .append("positions", token.positions)
                    .append("tags", token.tags)
                    .append("title", docTitle); // Store title for reference

            Document setFields = new Document("updated_at", new Date())
                    .append("postings." + docId, docPosting); // Use document ID as the key

            UpdateOneModel<Document> updateModel = new UpdateOneModel<>(
                    Filters.eq("term", term),
                    new Document()
                            .append("$setOnInsert", new Document("term", term))
                            .append("$set", setFields),
                    new UpdateOptions().upsert(true)
            );

            updates.add(updateModel);
        }

        if (!updates.isEmpty()) {
            indexCollection.bulkWrite(updates);
            System.out.println("[DEBUG] Tokens successfully inserted into inverted_index.");
        }

        // Update the document to mark it as indexed
        docCollection.updateOne(
            Filters.eq("_id", new ObjectId(docId)),
            Updates.set("isIndexed", true)
        );
        System.out.println("[DEBUG] Document marked as indexed: " + docTitle);
    }

    public List<String> getDocumentsForWord(String word) {
        List<String> docIds = new ArrayList<>();
        Document query = new Document("term", word);
        Document result = indexCollection.find(query).first();
        if (result != null) {
            Document postings = result.get("postings", Document.class);
            if (postings != null) {
                docIds.addAll(postings.keySet());
            }
        }
        return docIds;
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

}

