package com.okf;

public class SearchResultDTO {

    private String id;
    private String title;
    private int score;

    public SearchResultDTO(
            String id,
            String title,
            int score) {

        this.id = id;
        this.title = title;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getScore() {
        return score;
    }
}