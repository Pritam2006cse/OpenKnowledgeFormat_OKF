package com.okf;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkV6 {

    private static final int[] DOCUMENT_SIZES = {
            100,
            1000,
            5000,
            10000,
            50000
    };

    private static final String QUERY =
            "electric vehicle battery";

    private static final int WARMUP_RUNS = 100;
    private static final int MEASUREMENT_RUNS = 1000;

    public static void main(String[] args) {

        System.out.println();
        System.out.println(
                "=========================================================================="
        );
        System.out.println(
                "                    OKF LEVEL 2 BENCHMARK"
        );
        System.out.println(
                "=========================================================================="
        );

        System.out.println("Query: " + QUERY);
        System.out.println();

        System.out.printf(
                "%-10s %-14s %-14s %-14s %-14s %-12s%n",
                "Documents",
                "V3 Index",
                "V6 Index",
                "V3 Query",
                "V6 Query",
                "Slowdown"
        );

        System.out.println(
                "--------------------------------------------------------------------------"
        );

        for (int size : DOCUMENT_SIZES) {

            List<Document> documents =
                    generateDocuments(size);

            // =====================================================
            // V3 INDEX
            // =====================================================

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

            // =====================================================
            // V6 INDEX
            // =====================================================

            InvertedIndex v6Index =
                    new InvertedIndex();

            long v6IndexStart =
                    System.nanoTime();

            v6Index.buildIndex(documents);

            long v6IndexEnd =
                    System.nanoTime();

            double v6IndexTime =
                    (v6IndexEnd - v6IndexStart)
                            / 1_000_000.0;

            SearchEngine v6SearchEngine =
                    new SearchEngine(
                            documents,
                            v6Index
                    );

            // =====================================================
            // WARMUP
            // =====================================================

            for (int i = 0; i < WARMUP_RUNS; i++) {

                v3SearchEngine.search(QUERY);
                v6SearchEngine.search(QUERY);
            }

            // =====================================================
            // V3 QUERY TIME
            // =====================================================

            long v3Start =
                    System.nanoTime();

            for (int i = 0; i < MEASUREMENT_RUNS; i++) {

                v3SearchEngine.search(QUERY);
            }

            long v3End =
                    System.nanoTime();

            double v3QueryTime =
                    (v3End - v3Start)
                            / 1_000_000.0
                            / MEASUREMENT_RUNS;

            // =====================================================
            // V6 QUERY TIME
            // =====================================================

            long v6Start =
                    System.nanoTime();

            for (int i = 0; i < MEASUREMENT_RUNS; i++) {

                v6SearchEngine.search(QUERY);
            }

            long v6End =
                    System.nanoTime();

            double v6QueryTime =
                    (v6End - v6Start)
                            / 1_000_000.0
                            / MEASUREMENT_RUNS;

            // =====================================================
            // SLOWDOWN
            // =====================================================

            double slowdown =
                    v6QueryTime / v3QueryTime;

            // =====================================================
            // OUTPUT
            // =====================================================

            System.out.printf(
                    "%-10d %-14.4f %-14.4f %-14.6f %-14.6f %-12.2fx%n",
                    size,
                    v3IndexTime,
                    v6IndexTime,
                    v3QueryTime,
                    v6QueryTime,
                    slowdown
            );
        }

        System.out.println(
                "=========================================================================="
        );

        System.out.println(
                "\nBenchmark completed."
        );
    }


    // =============================================================
    // SYNTHETIC DOCUMENT GENERATOR
    // =============================================================

    private static List<Document> generateDocuments(
            int count) {

        List<Document> documents =
                new ArrayList<>();

        for (int i = 0; i < count; i++) {

            String content;

            if (i % 10 == 0) {

                content =
                        "# Electric Vehicle\n\n"
                        + "Electric vehicles use batteries "
                        + "for energy storage. "
                        + "Modern electric vehicles use "
                        + "advanced battery technology. "
                        + "Electric vehicles are becoming "
                        + "popular for transportation.";

            } else if (i % 5 == 0) {

                content =
                        "# Laptop\n\n"
                        + "Modern laptops use battery "
                        + "technology for portable computing. "
                        + "Laptop computers provide "
                        + "portable performance.";

            } else if (i % 3 == 0) {

                content =
                        "# Smartphone\n\n"
                        + "Smartphones use batteries "
                        + "for portable operation. "
                        + "Battery technology improves "
                        + "mobile devices.";

            } else {

                content =
                        "# Computer Document\n\n"
                        + "This document contains general "
                        + "computer and networking information.";
            }

            documents.add(
                    new Document(
                            "doc_" + i,
                            "Document " + i,
                            content
                    )
            );
        }

        return documents;
    }
}