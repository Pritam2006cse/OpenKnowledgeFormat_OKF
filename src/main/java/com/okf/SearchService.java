
package com.okf;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private volatile SearchEngine searchEngine;
    private volatile SemanticSearchEngine semanticSearchEngine;

    private final MarkdownMetadataService metadataService;

    public SearchService(MarkdownMetadataService metadataService)
            throws Exception {

        this.metadataService = metadataService;
        reloadIndex();
    }

    public synchronized void reloadIndex() throws Exception {

        System.out.println("Reloading OKF search index...");

        // -----------------------------
        // Step 1: Generate YAML metadata
        // -----------------------------

        metadataService.generateMetadataForAll("knowledge");

        System.out.println("Markdown metadata generation completed.");

        // -----------------------------
        // Step 2: Load Markdown documents
        // -----------------------------

        MarkdownLoader loader = new MarkdownLoader();

        List<Document> documents =
                loader.loadDocuments("knowledge");

        // -----------------------------
        // Step 3: Build keyword index
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
        // Step 4: Build semantic index
        // -----------------------------

        SemanticIndex semanticIndex =
                new SemanticIndex();

        semanticIndex.buildIndex(documents);

        // -----------------------------
        // Step 5: Generate related links
        // -----------------------------

        MarkdownLinkGenerator linkGenerator =
                new MarkdownLinkGenerator();

        linkGenerator.generateLinks(
                semanticIndex.getDocumentEmbeddings()
        );

        // -----------------------------
        // Step 6: Initialize semantic search
        // -----------------------------

        semanticSearchEngine =
                new SemanticSearchEngine(semanticIndex);

        // -----------------------------
        // Completion logs
        // -----------------------------

        System.out.println("OKF Search Engine initialized.");

        System.out.println(
                "Documents loaded: " + documents.size()
        );

        System.out.println(
                "Search indexes reloaded successfully."
        );
    }

    // Level 2: Keyword Search
    public List<SearchResult> search(String query) {

        return searchEngine.search(query);
    }

    // Level 3: Semantic Search
    public List<SearchResult> semanticSearch(
            String query) throws Exception {

        return semanticSearchEngine.search(query);
    }
}
