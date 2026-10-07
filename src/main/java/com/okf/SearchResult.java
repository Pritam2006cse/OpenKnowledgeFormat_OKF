package com.okf;

public class SearchResult {
    private Document document;
    private double score;
    public SearchResult(Document document, double score){
        this.document = document;
        this.score = score;
    }
    public Document getDocument(){
        return document;
    }
    public double getScore(){
        return score;
    }
}
