package com.okf;

import java.util.List;

public class EmbeddingTest {

    public static void main(String[] args) {

        OllamaEmbeddingService embeddingService =
                new OllamaEmbeddingService();

        String query =
                "rechargeable energy storage for vehicles";

        System.out.println("Generating query embedding...");

        List<Double> embedding =
                embeddingService.getQueryEmbedding(query);

        System.out.println("Embedding generated successfully.");
        System.out.println("Dimensions: " + embedding.size());

        System.out.println("\nFirst 10 values:");

        for (int i = 0; i < Math.min(10, embedding.size()); i++) {
            System.out.println(
                    i + ": " + embedding.get(i)
            );
        }
    }
}