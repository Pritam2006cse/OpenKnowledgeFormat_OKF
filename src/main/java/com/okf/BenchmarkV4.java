package com.okf;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkV4 {

    public static void main(String[] args) {

        int[] documentSizes = {
                5,
                100,
                1000,
                5000,
                10000
        };

        String query = "battery";

        System.out.println("====================================");
        System.out.println("        OKF V4 BENCHMARK");
        System.out.println("====================================");

        System.out.println();

        System.out.printf(
                "%-12s %-20s %-20s %-12s%n",
                "Documents",
                "Index Build (ms)",
                "Query Time (ms)",
                "Results"
        );

        System.out.println(
                "------------------------------------------------------------------"
        );

        for (int size : documentSizes) {

            // Create test documents
            List<Document> documents =
                    createTestDocuments(size);

            // Measure index construction time
            long indexStart = System.nanoTime();

            SearchEngine searchEngine =
                    new SearchEngine(documents);

            long indexEnd = System.nanoTime();

            double indexTime =
                    (indexEnd - indexStart)
                    / 1_000_000.0;

            // Warm-up query
            searchEngine.search(query);

            // Measure query time
            long queryStart = System.nanoTime();

            List<SearchResult> results =
                    searchEngine.search(query);

            long queryEnd = System.nanoTime();

            double queryTime =
                    (queryEnd - queryStart)
                    / 1_000_000.0;

            // Display result
            System.out.printf(
                    "%-12d %-20.4f %-20.4f %-12d%n",
                    size,
                    indexTime,
                    queryTime,
                    results.size()
            );
        }

        System.out.println();
        System.out.println("Benchmark completed.");
    }

    private static List<Document> createTestDocuments(
            int numberOfDocuments) {

        List<Document> documents =
                new ArrayList<>();

        for (int i = 1; i <= numberOfDocuments; i++) {

            String title =
                    "Test Document " + i;

            String content;

            /*
             * Every 10th document contains "battery".
             * This gives us predictable search results.
             */
            if (i % 10 == 0) {

                content =
                        "This document contains information "
                        + "about battery technology, energy "
                        + "storage and electric systems.";

            } else {

                content =
                        "This is a test document containing "
                        + "general information about computers "
                        + "and technology.";
            }

           documents.add(
        new Document(
                String.valueOf(i),
                title,
                content
        )
);
        }

        return documents;
    }
}
