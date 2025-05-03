package com.example.demo;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import processor.QueryProcessor;
import db.DBManager;
import processor.QueryResult;
import ranker.RankedDocument;

import java.util.List;

import static ranker.Ranker.RankerMain;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class SearchController {
    public static class SearchResult {
        private final List<RankedDocument> results;
        private final long totalCount;
        private final long totalTime;

        public SearchResult(List<RankedDocument> results, long totalCount, long totalTime) {
            this.results = results;
            this.totalCount = totalCount;
            this.totalTime = totalTime;
        }

        public List<RankedDocument> getResults() {
            return results;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public long getTotalTime() {
            return totalTime;
        }
    }


    private final QueryProcessor queryProcessor;
    private final DBManager dbManager=new DBManager();

    public SearchController() {
        this.queryProcessor = new QueryProcessor(dbManager);
    }

    @GetMapping("/search")
    public SearchResult search(@RequestParam String query,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {

         long start1 = System.currentTimeMillis();
         QueryResult queryresult = queryProcessor.processQuery(query, page, size);
         long duration1 = System.currentTimeMillis() - start1;
         System.out.println("Query processing time: " + duration1 + " ms");
        System.out.println("query time" + queryresult.getTime());


         long start2 = System.currentTimeMillis();
         List<RankedDocument> results =  RankerMain(queryresult,dbManager);
         long duration2 = System.currentTimeMillis() - start2;

         System.out.println("Ranking time: " + duration2 + " ms");

        return new SearchResult(results, queryresult.getTotalCount(), duration1 + duration2);
    }

}