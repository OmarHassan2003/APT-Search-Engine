package processor;

import java.util.Map;

public class DocInfo {
    private Map<String, Map<String, Object>> wordInfo;

    public Map<String, Map<String, Object>> getWordInfo() {
        return wordInfo;
    }

    public void setWordInfo(Map<String, Map<String, Object>> wordInfo) {
        this.wordInfo = wordInfo;
    }
}
