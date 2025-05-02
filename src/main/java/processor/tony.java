package processor;


import db.DBManager;

import java.util.List;

public class tony {
    private static final DBManager db = new DBManager();
    public static void main(String[] args) {
        System.out.println("Running the Query Processor...");

        //IndexAccess indexAccess = new IndexAccess(connectionString, databaseName, collectionName);
        QueryProcessor processor = new QueryProcessor(db);

        // 2. Test your queries
        testQuery(processor, "travel guide");
        testQuery(processor, "\"travel guide\"");
        testQuery(processor, "\"travel guide\" AND \"europe tips\"");
        testQuery(processor, "\"summer vacat\" AND \"vacat plan\"");
        testQuery(processor, "\"summer vacat\" OR \"fun\"");
        testQuery(processor, "\"vacat plan\" NOT \"summer vacat\"");
        testQuery(processor, "\"summer\"");
        testQuery(processor,  "summer");
        testQuery(processor, "\"vacat plan\"");


        System.out.println("finished");
        db.close();
    }

    public static void testQuery(QueryProcessor processor, String query) {
        System.out.println("Query: " + query);
        QueryResult result = processor.processQuery(query,0,10);
        System.out.println("docIDs: " + result.getDocIds());
        System.out.println("docData: " + result.getDocData());
        System.out.println("Query Type: " + result.getType());
        System.out.println("Query Time: " + result.getTime() + " ms");
        System.out.println("Query Words: " + result.getQueryWords());
        System.out.println("Query Words String: " + result.getQueryWordsString());
        System.out.println("---------------------------------------------------");
    }
}