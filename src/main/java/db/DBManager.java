package db;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;

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
                                       .append("positions", token.positions)
                                       .append("tags", token.tags); // Added tags field

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
