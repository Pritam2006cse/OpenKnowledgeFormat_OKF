package com.okf;

import java.util.*;

public class InvertedIndex {

    private Map<String, Set<String>> index;

    public InvertedIndex() {
        index = new HashMap<>();
    }

    
    public void buildIndex(List<Document> documents) {
        for (Document document : documents) {
            List<String> terms = TextPreprocessor.process(document.getContent());
            for (String term : terms) {
                index.putIfAbsent(term, new HashSet<>());
                index.get(term).add(document.getId());
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