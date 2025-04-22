package src.processor;

import java.util.Set;
import java.util.List;

public class QueryResult {
    private final Set<String> documents;
    private final String type;
    private final long duration;
    private final List<String> queryWords;

    public QueryResult(Set<String> documents, String type, long duration, List<String> queryWords) {
        this.documents = documents;
        this.type = type;
        this.duration = duration;
        this.queryWords = queryWords;
    }

    public Set<String> getDocuments() {
        return documents;
    }

    public String getType() {
        return type;
    }

    public long getDuration() {
        return duration;
    }

    public List<String> getQueryWords() {
        return queryWords;
    }
}
