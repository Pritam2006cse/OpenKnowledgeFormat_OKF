package com.okf;

import java.util.List;

public class SemanticSearchTest {

    public static void main(String[] args) throws Exception {

        // 1. Load Markdown documents
        MarkdownLoader loader = new MarkdownLoader();

        List<Document> documents =
                loader.loadDocuments("knowledge");

        System.out.println(
                "Documents loaded: " + documents.size()
        );

        // 2. Build semantic index
        SemanticIndex semanticIndex =
                new SemanticIndex();

        long indexStart = System.nanoTime();

        semanticIndex.buildIndex(documents);

        long indexEnd = System.nanoTime();

        double indexTime =
                (indexEnd - indexStart) / 1_000_000.0;

        System.out.println(
                "Index construction time: "
                + indexTime
                + " ms"
        );

        // 3. Create semantic search engine
        SemanticSearchEngine searchEngine =
                new SemanticSearchEngine(
                        semanticIndex
                );

        // 4. Test semantic query
        String query =
                "machines that can learn and make decisions";

        System.out.println("\nQuery: " + query);

        // 5. Perform semantic search
        long searchStart = System.nanoTime();

        List<SearchResult> results =
                searchEngine.search(query);

        long searchEnd = System.nanoTime();

        double searchTime =
                (searchEnd - searchStart) / 1_000_000.0;

        // 6. Display results
        System.out.println("\nSemantic Results:");

        int rank = 1;

        for (SearchResult result : results) {

            System.out.println(
                    rank
                    + ". "
                    + result.getDocument().getTitle()
                    + " | Similarity: "
                    + String.format(
                            "%.4f",
                            result.getScore()
                    )
            );

            rank++;
        }

        System.out.println(
                "\nSemantic retrieval time: "
                + searchTime
                + " ms"
        );
    }
}