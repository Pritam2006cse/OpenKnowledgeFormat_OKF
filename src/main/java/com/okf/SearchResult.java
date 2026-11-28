package com.okf;

public class SearchResult {

    private Document document;
    private double score;
    private String content;

    public SearchResult(
            Document document,
            double score) {

        this.document = document;
        this.score = score;
    }

    public SearchResult(
            Document document,
            double score,
            String content) {

        this.document = document;
        this.score = score;
        this.content = content;
    }

    public Document getDocument() {
        return document;
    }

    public double getScore() {
        return score;
    }

    public String getContent() {
        return content;
    }
}