package com.okf;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private volatile SearchEngine searchEngine;

    public SearchService() throws Exception {
        reloadIndex();
    }

    public synchronized void reloadIndex() throws Exception {

        System.out.println("Reloading OKF search index...");

        // 1. Load all Markdown documents
        MarkdownLoader loader = new MarkdownLoader();

        List<Document> documents =
                loader.loadDocuments("knowledge");

        // 2. Build a fresh inverted index
        InvertedIndex invertedIndex =
                new InvertedIndex();

        invertedIndex.buildIndex(documents);

        // 3. Create a new search engine
        searchEngine =
                new SearchEngine(
                        documents,
                        invertedIndex
                );

        System.out.println(
                "OKF Search Engine initialized."
        );

        System.out.println(
                "Documents loaded: "
                + documents.size()
        );

        System.out.println(
                "Search index reloaded successfully."
        );
    }

    public List<SearchResult> search(String query) {

        return searchEngine.search(query);
    }
}