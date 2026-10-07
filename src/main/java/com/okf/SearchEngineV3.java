package com.okf;

import java.util.*;

public class SearchEngineV3 {

    private InvertedIndexV3 invertedIndex;
    private Map<String, Document> documentMap;

    public SearchEngineV3(
            List<Document> documents,
            InvertedIndexV3 invertedIndex) {

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

        Map<String, Double> scores =
                new HashMap<>();

        String[] queryTerms =
                query.toLowerCase().split("\\s+");

        for (String term : queryTerms) {

            if (term.isEmpty()) {
                continue;
            }

            Set<String> documentIds =
                    invertedIndex.getDocuments(term);

            for (String documentId : documentIds) {

                scores.put(
                        documentId,
                        scores.getOrDefault(
                                documentId,
                                0.0
                        ) + 1.0
                );
            }
        }

        List<SearchResult> results =
                new ArrayList<>();

        for (Map.Entry<String, Double> entry
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

        results.sort(
                Comparator.comparingDouble(
                        SearchResult::getScore
                ).reversed()
        );

        return results;
    }
}