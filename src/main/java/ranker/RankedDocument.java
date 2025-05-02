package ranker;

public class RankedDocument {
  private String url;
  private Double score;
  private String title;
  private String snippet;

  public RankedDocument(String url, Double score, String title, String snippet) {
    this.url = url;
    this.score = score;
    this.title = title;
    this.snippet = snippet;
  }

  public String getUrl() {
    return url;
  }

  public Double getScore() {
    return score;
  }

  public String getTitle() {
    return title;
  }

  public String getSnippet() {
    return snippet;
  }
}