package com.okf;

import java.util.*;

public class SearchEngine {

    private InvertedIndex invertedIndex;

    // Maps document ID -> actual Document
    private Map<String, Document> documentMap;

    public SearchEngine(
            List<Document> documents,
            InvertedIndex invertedIndex) {

        this.invertedIndex = invertedIndex;

        documentMap = new HashMap<>();

        for (Document document : documents) {
            documentMap.put(
                    document.getId(),
                    document
            );
        }
    }

    public List<SearchResult> search(String query) {

        // document ID -> score
        Map<String, Integer> scores = new HashMap<>();

        // Break query into words
        String[] queryTerms =
                query.toLowerCase().split("\\s+");

        // Search the inverted index
        for (String term : queryTerms) {

            if (term.isEmpty()) {
                continue;
            }

            // Get documents containing this word
            Set<String> documentIds =
                    invertedIndex.getDocuments(term);

            // Increase score for each matching document
            for (String documentId : documentIds) {

                scores.put(
                        documentId,
                        scores.getOrDefault(documentId, 0) + 1
                );
            }
        }

        // Convert scored document IDs into SearchResults
        List<SearchResult> results =
                new ArrayList<>();

        for (Map.Entry<String, Integer> entry
                : scores.entrySet()) {

            Document document =
                    documentMap.get(entry.getKey());

            if (document != null) {

                results.add(
                        new SearchResult(
                                document,
                                entry.getValue()
                        )
                );
            }
        }

        // Highest score first
        results.sort(
                Comparator.comparingInt(
                        SearchResult::getScore
                ).reversed()
        );

        return results;
    }
}