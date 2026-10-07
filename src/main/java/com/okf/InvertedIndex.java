package com.okf;

import java.util.*;

public class InvertedIndex {

    private Map<String, Map<String,Integer>> index;

    public InvertedIndex() {
        index = new HashMap<>();
    }

    public void buildIndex(List<Document> documents) {
        for (Document document : documents) {
            List<String> terms = TextPreprocessor.process(document.getContent());
            Map<String, Integer> termFrequency = new HashMap<>();
            for (String term : terms) {
                //index.putIfAbsent(term, new HashSet<>());
                //index.get(term).add(document.getId());
                termFrequency.put(term,termFrequency.getOrDefault(term, 0) + 1);
            }
            for (Map.Entry<String, Integer> entry: termFrequency.entrySet()) {
                String term = entry.getKey();
                int frequency = entry.getValue();
                index.putIfAbsent(term, new HashMap<>());
                index.get(term).put(document.getId(),frequency);
            }
        }
    }

    public Set<String> getDocuments(String word) {
        return index.getOrDefault( word.toLowerCase(), Collections.emptyMap()).keySet();
    }

    public int getTermFrequency(String term,String documentId) {
        return index.getOrDefault(term.toLowerCase(),Collections.emptyMap()).getOrDefault(documentId, 0);
    }

    public int getDocumentFrequency(String term) {
        return index.getOrDefault(term.toLowerCase(),Collections.emptyMap()).size();
    }

    public void displayIndex() {
        for (Map.Entry<String, Map<String,Integer>> entry: index.entrySet()) {
            System.out.println(entry.getKey()+ " -> "+ entry.getValue());
        }
    }
}