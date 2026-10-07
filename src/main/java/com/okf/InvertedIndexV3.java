package com.okf;

import java.util.*;

public class InvertedIndexV3 {

    // term -> set of document IDs
    private Map<String, Set<String>> index;

    public InvertedIndexV3() {
        index = new HashMap<>();
    }

    public void buildIndex(List<Document> documents) {

        for (Document document : documents) {

            String content =
                    TextPreprocessor.cleanMarkdown(
                            document.getContent()
                    );

            String[] words =
                    content.toLowerCase().split("\\W+");

            for (String word : words) {

                if (word.isEmpty()) {
                    continue;
                }

                index.putIfAbsent(
                        word,
                        new HashSet<>()
                );

                index.get(word).add(
                        document.getId()
                );
            }
        }
    }

    public Set<String> getDocuments(String word) {

        return index.getOrDefault(
                word.toLowerCase(),
                Collections.emptySet()
        );
    }
}