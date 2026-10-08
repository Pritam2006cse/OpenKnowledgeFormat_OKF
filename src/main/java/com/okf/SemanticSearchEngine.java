package com.okf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SemanticSearchEngine {

    private final OllamaEmbeddingService embeddingService;
    private final SemanticIndex semanticIndex;

    public SemanticSearchEngine(SemanticIndex semanticIndex) {
        this.embeddingService = new OllamaEmbeddingService();
        this.semanticIndex = semanticIndex;
    }

    public List<SearchResult> search(String query)
            throws Exception {

        List<Double> queryEmbedding =
                embeddingService.getQueryEmbedding(query);

        if (queryEmbedding.isEmpty()) {
            return new ArrayList<>();
        }

        List<SearchResult> results = new ArrayList<>();

        for (DocumentEmbedding documentEmbedding :
                semanticIndex.getDocumentEmbeddings()) {

            double similarity =
                    cosineSimilarity(
                            queryEmbedding,
                            documentEmbedding.getEmbedding()
                    );

            results.add(
                    new SearchResult(
                            documentEmbedding.getDocument(),
                            similarity
                    )
            );
        }

        results.sort(
                Comparator.comparingDouble(
                        SearchResult::getScore
                ).reversed()
        );

        return results;
    }

    private double cosineSimilarity(
            List<Double> vectorA,
            List<Double> vectorB) {

        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException(
                    "Vectors must have the same dimensions."
            );
        }

        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {

            double a = vectorA.get(i);
            double b = vectorB.get(i);

            dotProduct += a * b;
            magnitudeA += a * a;
            magnitudeB += b * b;
        }

        if (magnitudeA == 0.0 || magnitudeB == 0.0) {
            return 0.0;
        }

        return dotProduct /
                (Math.sqrt(magnitudeA)
                * Math.sqrt(magnitudeB));
    }
}