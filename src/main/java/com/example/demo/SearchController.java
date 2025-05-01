package com.example.demo;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import processor.QueryProcessor;
import db.DBManager;
import processor.QueryResult;


@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class SearchController {

    private final QueryProcessor queryProcessor;

    public SearchController() {
        DBManager dbManager = new DBManager();
        this.queryProcessor = new QueryProcessor(dbManager);
    }

    @GetMapping("/search")
    public QueryResult search(@RequestParam String query) {
        QueryResult queryResult =  queryProcessor.processQuery(query);

        return queryResult;
    }

}