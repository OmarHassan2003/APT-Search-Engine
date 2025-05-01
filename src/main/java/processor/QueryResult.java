package processor;

import java.util.List;
import java.util.Map;

public class QueryResult {
    private List<String> docIds;
    private List<Map<String, Object>> docData;
    private String type;
    private long time;
    private List<String> queryWords;
    private List<String> queryWordsString;

    public QueryResult() {
    }

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
}