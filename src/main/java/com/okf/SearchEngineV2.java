package com.okf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SearchEngineV2 {

    private List<Document> documents;

    public SearchEngineV2(
            List<Document> documents) {

        this.documents = documents;
    }


    public List<SearchResult> search(
            String query) {

        List<SearchResult> results =
                new ArrayList<>();


        String[] queryTerms =
                query.toLowerCase()
                        .split("\\s+");


        // Scan every document
        for (Document document : documents) {

            String content =
                    document.getContent()
                            .toLowerCase();


            int score = 0;


            // Check every query term
            for (String term : queryTerms) {

                if (content.contains(term)) {

                    score++;
                }
            }


            // Add matching documents
            if (score > 0) {

                results.add(
                        new SearchResult(
                                document,
                                score
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