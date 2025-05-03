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


        System.out.println("finished");
        db.close();
    }

    public static void testQuery(QueryProcessor processor, String query) {
        System.out.println("Query: " + query);
        QueryResult result = processor.processQuery(query,0,10);
        System.out.println("Total Count: " + result.getTotalCount());
        System.out.println("Time: " + result.getTime() + " ms");
        System.out.println("Doc IDs: " + result.getDocIds());
        System.out.println("Doc Data: " + result.getDocData());
        System.out.println("Query Words: " + result.getQueryWords());
        System.out.println("Query Words String: " + result.getQueryWordsString());
        System.out.println("Type: " + result.getType());
        System.out.println("Per Word Results: " + result.getPerWordResults());


        System.out.println("---------------------------------------------------");
    }
}