package com.okf;

import java.util.List;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) throws Exception {

    // 1. Load Markdown files
    MarkdownLoader loader = new MarkdownLoader();
    List<Document> documents = loader.loadDocuments("knowledge");
    System.out.println("Documents loaded: " + documents.size());

    // 2. Build inverted index
    InvertedIndex invertedIndex = new InvertedIndex();
    long indexStart = System.nanoTime();
    invertedIndex.buildIndex(documents);
    long indexEnd = System.nanoTime();
    double indexTime = (indexEnd - indexStart) / 1_000_000.0;

    System.out.println("\nIndex built successfully.");

    System.out.println("Index construction time: "+ indexTime+ " ms");

    // 2. Create search engine
    SearchEngine searchEngine = new SearchEngine(documents,invertedIndex);

    Scanner sc = new Scanner(System.in);
    System.out.print("\nEnter your query: ");
    // 3. Query
    String query = sc.nextLine();
    System.out.println("\nQuery: " + query);

    // 4. Measure retrieval time
    long start = System.nanoTime();
    List<SearchResult> results = searchEngine.search(query);
    long end = System.nanoTime();

    // 5. Display results
    System.out.println("\nResults:");

    int rank = 1;

    for (SearchResult result : results) {
      System.out.println(rank + ". " + result.getDocument().getTitle() + " | Score: " + result.getScore());
      rank++;
    }

    // 6. Performance
    double timeMs = (end - start) / 1_000_000.0;
    System.out.println("\nRetrieval time: " + timeMs + " ms");
  }
}