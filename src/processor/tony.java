package src.processor;


public class tony {
    public static void main(String[] args) {
        // 1. Connect to your MongoDB inverted index
        String connectionString = "mongodb://localhost:27017";
        String databaseName = "search_index";
        String collectionName = "terms";

        IndexAccess indexAccess = new IndexAccess(connectionString, databaseName, collectionName);
        QueryProcessor processor = new QueryProcessor(indexAccess);

        // 2. Test your queries
        testQuery(processor, "\"travel guide\"");
        testQuery(processor, "travel guide");
        testQuery(processor, "\"travel guide\" AND \"europe tips\"");
        testQuery(processor, "\"summer vacat\" AND \"vacat plan\"");
        testQuery(processor, "\"summer vacat\" OR \"fun\"");
        testQuery(processor, "\"vacat plan\" NOT \"summer vacat\"");
            testQuery(processor, "\"summer\"");
       testQuery(processor, "\"vacat plan\"");
       // ( travel "europe jdslk fjlkj" )


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
