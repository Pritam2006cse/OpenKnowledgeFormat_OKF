package com.okf;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final SearchEngine searchEngine;
    public SearchService() throws Exception {
        MarkdownLoader loader = new MarkdownLoader();
        List<Document> documents = loader.loadDocuments("knowledge");
        InvertedIndex invertedIndex = new InvertedIndex();
        invertedIndex.buildIndex(documents);
        searchEngine = new SearchEngine(documents, invertedIndex);
        System.out.println("OKF Search Engine initialized.");
        System.out.println("Documents loaded: " + documents.size());
    }

    public List<SearchResult> search(String query) {
        return searchEngine.search(query);
    }
}