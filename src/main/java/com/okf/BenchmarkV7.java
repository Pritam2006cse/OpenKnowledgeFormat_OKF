package com.okf;

import java.util.List;

public class BenchmarkV7 {

    private static final String[] QUERIES = {
            "machines that can learn and make decisions",
            "rechargeable energy storage for vehicles",
            "devices that communicate over networks"
    };

    private static final int WARMUP_RUNS = 2;
    private static final int MEASUREMENT_RUNS = 5;

    public static void main(String[] args) throws Exception {

        System.out.println();
        System.out.println("==========================================================================");
        System.out.println("                         OKF LEVEL 3 BENCHMARK");
        System.out.println("==========================================================================");

        // ------------------------------------------------------------
        // Load documents
        // ------------------------------------------------------------

        MarkdownLoader loader = new MarkdownLoader();

        List<Document> documents =
                loader.loadDocuments("knowledge");

        System.out.println(
                "Documents loaded: " + documents.size()
        );

        // ------------------------------------------------------------
        // V3
        // ------------------------------------------------------------

        System.out.println("\nBuilding V3 inverted index...");

        InvertedIndexV3 v3Index =
                new InvertedIndexV3();

        long v3IndexStart =
                System.nanoTime();

        v3Index.buildIndex(documents);

        long v3IndexEnd =
                System.nanoTime();

        double v3IndexTime =
                (v3IndexEnd - v3IndexStart)
                        / 1_000_000.0;

        SearchEngineV3 v3SearchEngine =
                new SearchEngineV3(
                        documents,
                        v3Index
                );

        // ------------------------------------------------------------
        // Level 2
        // ------------------------------------------------------------

        System.out.println("Building Level 2 TF-IDF index...");

        InvertedIndex level2Index =
                new InvertedIndex();

        long level2IndexStart =
                System.nanoTime();

        level2Index.buildIndex(documents);

        long level2IndexEnd =
                System.nanoTime();

        double level2IndexTime =
                (level2IndexEnd - level2IndexStart)
                        / 1_000_000.0;

        SearchEngine level2SearchEngine =
                new SearchEngine(
                        documents,
                        level2Index
                );

        // ------------------------------------------------------------
        // Level 3
        // ------------------------------------------------------------

        System.out.println(
                "\nLoading / updating Level 3 semantic index..."
        );

        SemanticIndex semanticIndex =
                new SemanticIndex();

        long semanticIndexStart =
                System.nanoTime();

        semanticIndex.buildIndex(documents);

        long semanticIndexEnd =
                System.nanoTime();

        double semanticIndexTime =
                (semanticIndexEnd - semanticIndexStart)
                        / 1_000_000.0;

        SemanticSearchEngine semanticSearchEngine =
                new SemanticSearchEngine(
                        semanticIndex
                );

        // ------------------------------------------------------------
        // Print index construction times
        // ------------------------------------------------------------

        System.out.println();
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("INDEX CONSTRUCTION / UPDATE");
        System.out.println("--------------------------------------------------------------------------");

        System.out.printf(
                "V3 Inverted Index       : %.4f ms%n",
                v3IndexTime
        );

        System.out.printf(
                "Level 2 TF-IDF Index    : %.4f ms%n",
                level2IndexTime
        );

        System.out.printf(
                "Level 3 Semantic Index  : %.4f ms%n",
                semanticIndexTime
        );

        // ------------------------------------------------------------
        // Query benchmark
        // ------------------------------------------------------------

        System.out.println();
        System.out.println("==========================================================================");
        System.out.println("QUERY PERFORMANCE");
        System.out.println("==========================================================================");

        System.out.printf(
                "%-45s %-14s %-14s %-14s%n",
                "Query",
                "V3 (ms)",
                "Level 2 (ms)",
                "Level 3 (ms)"
        );

        System.out.println(
                "--------------------------------------------------------------------------"
        );

        for (String query : QUERIES) {

            // --------------------------------------------------------
            // Warmup
            // --------------------------------------------------------

            for (int i = 0; i < WARMUP_RUNS; i++) {

                v3SearchEngine.search(query);

                level2SearchEngine.search(query);

                semanticSearchEngine.search(query);
            }

            // --------------------------------------------------------
            // V3 benchmark
            // --------------------------------------------------------

            long v3Start =
                    System.nanoTime();

            for (int i = 0; i < MEASUREMENT_RUNS; i++) {

                v3SearchEngine.search(query);
            }

            long v3End =
                    System.nanoTime();

            double v3QueryTime =
                    (v3End - v3Start)
                            / 1_000_000.0
                            / MEASUREMENT_RUNS;

            // --------------------------------------------------------
            // Level 2 benchmark
            // --------------------------------------------------------

            long level2Start =
                    System.nanoTime();

            for (int i = 0; i < MEASUREMENT_RUNS; i++) {

                level2SearchEngine.search(query);
            }

            long level2End =
                    System.nanoTime();

            double level2QueryTime =
                    (level2End - level2Start)
                            / 1_000_000.0
                            / MEASUREMENT_RUNS;

            // --------------------------------------------------------
            // Level 3 benchmark
            // --------------------------------------------------------

            long semanticStart =
                    System.nanoTime();

            for (int i = 0; i < MEASUREMENT_RUNS; i++) {

                semanticSearchEngine.search(query);
            }

            long semanticEnd =
                    System.nanoTime();

            double semanticQueryTime =
                    (semanticEnd - semanticStart)
                            / 1_000_000.0
                            / MEASUREMENT_RUNS;

            // --------------------------------------------------------
            // Print
            // --------------------------------------------------------

            System.out.printf(
                    "%-45s %-14.4f %-14.4f %-14.4f%n",
                    query,
                    v3QueryTime,
                    level2QueryTime,
                    semanticQueryTime
            );
        }

        // ------------------------------------------------------------
        // Semantic result verification
        // ------------------------------------------------------------

        System.out.println();
        System.out.println("==========================================================================");
        System.out.println("SEMANTIC RETRIEVAL VERIFICATION");
        System.out.println("==========================================================================");

        String verificationQuery =
                "machines that can learn and make decisions";

        System.out.println(
                "Query: " + verificationQuery
        );

        List<SearchResult> semanticResults =
                semanticSearchEngine.search(
                        verificationQuery
                );

        System.out.println();

        int rank = 1;

        for (SearchResult result : semanticResults) {

            System.out.printf(
                    "%d. %-30s %.4f%n",
                    rank,
                    result.getDocument().getTitle(),
                    result.getScore()
            );

            rank++;

            if (rank > 5) {
                break;
            }
        }

        System.out.println();
        System.out.println("==========================================================================");
        System.out.println("Benchmark completed.");
        System.out.println("==========================================================================");
    }
}