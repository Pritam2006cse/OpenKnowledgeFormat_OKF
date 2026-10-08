package com.okf;

import java.util.List;

public class DocumentEmbedding {

    private final Document document;
    private final List<Double> embedding;

    public DocumentEmbedding(
            Document document,
            List<Double> embedding) {

        this.document = document;
        this.embedding = embedding;
    }

    public Document getDocument() {
        return document;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }
}