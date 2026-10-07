package com.okf;
import java.util.*;

public class SearchEngine {
        private InvertedIndex invertedIndex;
        // Maps document ID -> actual Document
        private Map<String, Document> documentMap;
        private int totalDocuments;
        public SearchEngine(List<Document> documents,InvertedIndex invertedIndex) {
                this.invertedIndex = invertedIndex;
                this.totalDocuments = documents.size();
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
        Map<String, Double> scores = new HashMap<>();

        // Break query into words
        List<String> queryTerms = TextPreprocessor.process(query);

        // Search the inverted index
        for (String term : queryTerms) {
                Set<String> documentIds = invertedIndex.getDocuments(term);
                int documentFrequency = invertedIndex.getDocumentFrequency(term);
                if (documentFrequency == 0) {
                        continue;
                }
                double idf =Math.log((double) totalDocuments/ documentFrequency);
                // Increase score for each matching document
                for (String documentId : documentIds) {
                        int termFrequency = invertedIndex.getTermFrequency(term,documentId);
                        double tf = 1.0 + Math.log(termFrequency);
                        double tdidf = tf*idf;
                        scores.put(documentId,scores.getOrDefault(documentId, 0.0) + tdidf);
                }
        }

        // Convert scored document IDs into SearchResults
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            Document document = documentMap.get(entry.getKey());
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
        results.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());
        return results;
    }
}