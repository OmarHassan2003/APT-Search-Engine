package processor;


import db.DBManager;

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
        QueryResult result = processor.processQuery(query);
        System.out.println("Type: " + result.getType());
        System.out.println("Time Taken: " + result.getDuration() + " ms");
        System.out.println("Docs Found: " + result.getDocuments());
        System.out.println("Query Words: " + result.getQueryWords());
        System.out.println("---------------------------------------------------");
    }
}
