package src.processor;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class IndexAccess {
    private MongoCollection<Document> collection;

    public IndexAccess(String connectionString, String databaseName, String collectionName) {
        MongoClient mongoClient = MongoClients.create(connectionString);
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
    }

    // Get document IDs containing a specific term
    public List<String> getDocumentsForWord(String word) {
        List<String> docIds = new ArrayList<>();
        Document query = new Document("term", word);
        Document result = collection.find(query).first();
        if (result != null) {
            Document postings = (Document) result.get("postings");
            if (postings != null) {
                for (String docId : postings.keySet()) {
                    docIds.add(docId);
                }
            }
        }
        return docIds;
    }

    // Get positions of a term in a specific document
    public List<Integer> getPositionsForWord(String word, String docId) {
        List<Integer> positions = new ArrayList<>();
        Document query = new Document("term", word);
        Document result = collection.find(query).first();
        if (result != null) {
            Document postings = (Document) result.get("postings");
            if (postings != null && postings.containsKey(docId)) {
                positions = (List<Integer>) postings.get(docId);
            }
        }
        return positions;
    }
}