package com.okf;

import java.util.*;

public class InvertedIndex {

    private Map<String, Set<String>> index;

    public InvertedIndex() {
        index = new HashMap<>();
    }

    
    public void buildIndex(List<Document> documents) {
        for (Document document : documents) {
            String content = TextPreprocessor.cleanMarkdown(document.getContent());
            String[] words = content.split("\\W+");
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
        return index.getOrDefault( word.toLowerCase(), Collections.emptySet());
    }

    public void displayIndex() {
        for (Map.Entry<String, Set<String>> entry: index.entrySet()) {
            System.out.println(entry.getKey()+ " -> "+ entry.getValue());
        }
    }
}