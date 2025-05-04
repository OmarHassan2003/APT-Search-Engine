package processor;

import java.util.List;
import java.util.Map;

public class QueryResult {
    private List<String> docIds;
    private List<Map<String, Object>> docData; // Changed to a custom object for better clarity
    private String type;
    private long time;
    private List<String> queryWords;
    private List<String> queryWordsString;
    private long totalCount;  // Changed to long
    private Map<String, Map<String, Object>> perWordResults; // Added for ranker

    // Getters and setters

    public List<String> getDocIds() {
        return docIds;
    }

    public void setDocIds(List<String> docIds) {
        this.docIds = docIds;
    }

    public List<Map<String, Object>> getDocData() {
        return docData;
    }

    public void setDocData(List<Map<String, Object>> docData) {
        this.docData = docData;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public List<String> getQueryWords() {
        return queryWords;
    }

    public void setQueryWords(List<String> queryWords) {
        this.queryWords = queryWords;
    }

    public List<String> getQueryWordsString() {
        return queryWordsString;
    }

    public void setQueryWordsString(List<String> queryWordsString) {
        this.queryWordsString = queryWordsString;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public Map<String, Map<String, Object>> getPerWordResults() {
        return perWordResults;
    }

    public void setPerWordResults(Map<String, Map<String, Object>> perWordResults) {
        this.perWordResults = perWordResults;
    }


    // You can add more metadata fields like query time if needed
}
