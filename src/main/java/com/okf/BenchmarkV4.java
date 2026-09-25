package com.okf;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkV4 {

    public static void main(String[] args) {

        int[] documentSizes = {
                100,
                1000,
                5000,
                10000,
                50000
        };

        String query = "battery";
        int iterations = 100;
        System.out.println("              OKF V4 BENCHMARK");
        System.out.println();
        System.out.printf(
                "%-10s %-18s %-18s %-18s %-10s%n",
                "Documents",
                "V2 Avg (ms)",
                "Index Build (ms)",
                "V3 Avg (ms)",
                "Results"
        );

        System.out.println(
                "--------------------------------------------------------------------------"
        );

        for (int size : documentSizes) {
            // 1. Create synthetic documents
            List<Document> documents = createTestDocuments(size);

            // 2. Create V2 search engine
            SearchEngineV2 v2 = new SearchEngineV2(documents);

            // 3. Build V3 inverted index
            InvertedIndex invertedIndex = new InvertedIndex();

            long indexStart = System.nanoTime();
            invertedIndex.buildIndex(documents);
            long indexEnd = System.nanoTime();
            double indexBuildTime = (indexEnd - indexStart) / 1_000_000.0;

            // 4. Create V3 search engine
            SearchEngine v3 = new SearchEngine(documents,invertedIndex);

            // 5. Warm up JVM
            for (int i = 0; i < 10; i++) {
                v2.search(query);
                v3.search(query);
            }

            // 6. Benchmark V2
            long v2Start = System.nanoTime();
            List<SearchResult> v2Results = null;
            for (int i = 0; i < iterations; i++) {
                v2Results = v2.search(query);
            }
            long v2End = System.nanoTime();
            double v2Average = (v2End - v2Start)/ 1_000_000.0/ iterations;

            // 7. Benchmark V3

            long v3Start = System.nanoTime();
            List<SearchResult> v3Results = null;
            for (int i = 0; i < iterations; i++) {
                v3Results = v3.search(query);
            }
            long v3End = System.nanoTime();

            double v3Average = (v3End - v3Start)/ 1_000_000.0/ iterations;

            // 8. Display results
            System.out.printf(
                    "%-10d %-18.6f %-18.6f %-18.6f %-10d%n",
                    size,
                    v2Average,
                    indexBuildTime,
                    v3Average,
                    v3Results.size()
            );
        }


        System.out.println();
        System.out.println("Benchmark completed.");
    }

    // Create synthetic documents

    private static List<Document> createTestDocuments(
            int numberOfDocuments) {

        List<Document> documents = new ArrayList<>();


        for (int i = 1;i <= numberOfDocuments;i++) {

            String title = "Test Document " + i;
            String content;
            /*
             * Every 10th document contains
             * the word "battery".
             *
             * Therefore:
             *
             * 100 documents  -> 10 results
             * 1000 documents -> 100 results
             * 5000 documents -> 500 results
             * etc.
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

            documents.add(new Document(String.valueOf(i),title,content));
        }
        return documents;
    }
}