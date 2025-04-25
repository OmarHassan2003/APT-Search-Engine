package main.java.db;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import indexer.Tokenizer.Token;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

public class DBManager {
    private final MongoCollection<Document> docCollection;
    private final MongoCollection<Document> indexCollection;

    public DBManager() {
        String connectionString = "mongodb://localhost:27017";
        MongoClient mongoClient = MongoClients.create(connectionString);
        MongoDatabase database = mongoClient.getDatabase("search_engine");

        this.docCollection = database.getCollection("documents");
        this.indexCollection = database.getCollection("inverted_index");
    }

    /*
        TODO: 
        1- Crawler must add method to insert documents here
        2- Discuss with crawler how to know the position (h1, h2, title, etc)
        3- Dont forget Indexed tag ? true: false; (false by default from crawler)
    */


    // Fetch unindexed Documents
    public List<Document> getUnIndexedDocs(int limit) {
        List<Document> docs = new ArrayList<>();
        FindIterable<Document> results = docCollection.find(Filters.eq("indexed", false)).limit(limit);
    
        for (Document doc : results) docs.add(doc);
        
        return docs;
    }

    // Insert tokens into the inverted index
    public void insertInverted(String docId, HashMap<String, Token> tokens) {
        List<WriteModel<Document>> updates = new ArrayList<>();
    
        for (Map.Entry<String, Token> entry : tokens.entrySet()) {
            String term = entry.getKey();
            Token token = entry.getValue();
    
            Document docPosting = new Document("tf", token.count)
                                        .append("positions", token.positions);
    
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
        }
    
        if (!updates.isEmpty()) {
            indexCollection.bulkWrite(updates); // ⬅ bulk operation
        }
    
        docCollection.updateOne(Filters.eq("_id", new ObjectId(docId)), Updates.set("indexed", true));
    }
    
    
}
