package main.java.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;

public class MongoDBConnection {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String URI = dotenv.get("DB_URI");
    private static MongoClient mongoClient;

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(URI);
        }
        return mongoClient.getDatabase(dotenv.get("DB_NAME"));
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
}
