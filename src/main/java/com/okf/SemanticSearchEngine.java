package com.okf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SemanticSearchEngine {

    private final OllamaEmbeddingService embeddingService;
    private final SemanticIndex semanticIndex;
    private final MarkdownContentLoader contentLoader;

    public SemanticSearchEngine(
            SemanticIndex semanticIndex) {

        this.embeddingService =
                new OllamaEmbeddingService();

        this.semanticIndex =
                semanticIndex;

        this.contentLoader =
                new MarkdownContentLoader();
    }

    public List<SearchResult> search(
            String query)
            throws Exception {

        List<Double> queryEmbedding =
                embeddingService
                        .getQueryEmbedding(query);

        if (queryEmbedding.isEmpty()) {
            return new ArrayList<>();
        }

        /*
         * ------------------------------------------------
         * 1. SEMANTIC SEARCH
         * ------------------------------------------------
         */

        List<SearchResult> results =
                new ArrayList<>();

        for (DocumentEmbedding documentEmbedding :
                semanticIndex
                        .getDocumentEmbeddings()) {

            double similarity =
                    cosineSimilarity(
                            queryEmbedding,
                            documentEmbedding
                                    .getEmbedding()
                    );

            results.add(
                    new SearchResult(
                            documentEmbedding
                                    .getDocument(),
                            similarity
                    )
            );
        }

        /*
         * ------------------------------------------------
         * 2. FTS5 KEYWORD SEARCH
         * ------------------------------------------------
         */

        List<String> keywordMatches =
                semanticIndex.searchKeyword(query);

        Set<String> keywordMatchIds =
                new HashSet<>(
                        keywordMatches
                );

        System.out.println(
                "\n========== HYBRID SEARCH =========="
        );

        System.out.println(
                "Query: " + query
        );

        System.out.println(
                "FTS5 matches: "
                        + keywordMatches
        );

        /*
         * ------------------------------------------------
         * 3. COMBINE RESULTS
         * ------------------------------------------------
         */

        List<SearchResult> combinedResults =
                new ArrayList<>();

        for (SearchResult result :
                results) {

            String documentId =
                    result.getDocument()
                            .getId();

            double finalScore =
                    result.getScore();

            if (keywordMatchIds.contains(
                    documentId)) {

                System.out.println(
                        "FTS5 boost applied to: "
                                + documentId
                );

                finalScore += 0.10;
            }

            /*
             * Load the actual Markdown file.
             */
            String content =
                    contentLoader.load(
                            documentId
                    );
                System.out.println(
                        "Loaded Markdown: "
                                + documentId
                                + " | Characters: "
                                + content.length()
                );
            combinedResults.add(
                    new SearchResult(
                            result.getDocument(),
                            finalScore,
                            content
                    )
            );
        }

        /*
         * ------------------------------------------------
         * 4. SORT
         * ------------------------------------------------
         */

        combinedResults.sort(
                Comparator.comparingDouble(
                        SearchResult::getScore
                ).reversed()
        );

        /*
         * ------------------------------------------------
         * DEBUG OUTPUT
         * ------------------------------------------------
         */

        System.out.println(
                "Hybrid results:"
        );

        for (SearchResult result :
                combinedResults) {

            System.out.println(
                    result.getDocument()
                            .getId()
                    + " -> "
                    + result.getScore()
            );
        }

        System.out.println(
                "=================================\n"
        );

        return combinedResults;
    }

    private double cosineSimilarity(
            List<Double> vectorA,
            List<Double> vectorB) {

        if (vectorA.size()
                != vectorB.size()) {

            throw new IllegalArgumentException(
                    "Vectors must have the same dimensions."
            );
        }

        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int i = 0;
             i < vectorA.size();
             i++) {

            double a =
                    vectorA.get(i);

            double b =
                    vectorB.get(i);

            dotProduct +=
                    a * b;

            magnitudeA +=
                    a * a;

            magnitudeB +=
                    b * b;
        }

        if (magnitudeA == 0.0
                || magnitudeB == 0.0) {

            return 0.0;
        }

        return dotProduct /
                (
                        Math.sqrt(magnitudeA)
                                * Math.sqrt(
                                magnitudeB
                        )
                );
    }
}