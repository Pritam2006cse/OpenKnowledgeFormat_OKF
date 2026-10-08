package com.okf;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private volatile SearchEngine searchEngine;
    private volatile SemanticSearchEngine semanticSearchEngine;

    public SearchService() throws Exception {
        reloadIndex();
    }

    public synchronized void reloadIndex() throws Exception {

        System.out.println("Reloading OKF search index...");

        MarkdownLoader loader = new MarkdownLoader();

        List<Document> documents =
                loader.loadDocuments("knowledge");

        // -----------------------------
        // Level 2 index
        // -----------------------------

        InvertedIndex invertedIndex =
                new InvertedIndex();

        invertedIndex.buildIndex(documents);

        searchEngine =
                new SearchEngine(
                        documents,
                        invertedIndex
                );

        // -----------------------------
        // Level 3 semantic index
        // -----------------------------

        SemanticIndex semanticIndex =
                new SemanticIndex();

        semanticIndex.buildIndex(documents);

        semanticSearchEngine =
                new SemanticSearchEngine(
                        semanticIndex
                );

        System.out.println(
                "OKF Search Engine initialized."
        );

        System.out.println(
                "Documents loaded: "
                + documents.size()
        );

        System.out.println(
                "Search indexes reloaded successfully."
        );
    }

    // Level 2
    public List<SearchResult> search(String query) {

        return searchEngine.search(query);
    }

    // Level 3
    public List<SearchResult> semanticSearch(
            String query) throws Exception {

        return semanticSearchEngine.search(query);
    }
}