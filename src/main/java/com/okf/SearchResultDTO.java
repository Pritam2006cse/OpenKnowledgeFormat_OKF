package com.okf;

public class SearchResultDTO {

    private String id;
    private String title;
    private double score;
    private String snippet;

    public SearchResultDTO(
            String id,
            String title,
            double score,
            String snippet) {

        this.id = id;
        this.title = title;
        this.score = score;
        this.snippet = snippet;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public double getScore() {
        return score;
    }

    public String getSnippet(){
        return snippet;
    }
}