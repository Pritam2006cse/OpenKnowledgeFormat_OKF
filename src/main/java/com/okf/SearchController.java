package com.okf;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8081")
public class SearchController {

    private final SearchService searchService;
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    @GetMapping("/search")
    public List<SearchResultDTO> search(@RequestParam("q") String q) {
        return searchService.search(q)
                .stream()
                .map(result ->
                        new SearchResultDTO(
                                result.getDocument().getId(),
                                result.getDocument().getTitle(),
                                result.getScore(),
                                createSnippet(result.getDocument().getContent(),q)
                        )
                )
                .toList();
    }

    private String createSnippet(String content, String query) {
        if (content == null || content.isBlank()) {
            return "No content available.";
        }

        String clean = TextPreprocessor
                .cleanMarkdown(content)
                .replaceAll("\\s+", " ")
                .trim();

        if (clean.isBlank()) {
            return "No content available.";
        }

        String lowerContent = clean.toLowerCase();
        String lowerQuery = query.toLowerCase().trim();

        int position = lowerContent.indexOf(lowerQuery);

        if (position == -1) {
            return clean.length() > 180
                    ? clean.substring(0, 180) + "..."
                    : clean;
        }

        int start = Math.max(0, position - 70);
        int end = Math.min(clean.length(), position + lowerQuery.length() + 110);

        String snippet = clean.substring(start, end);

        if (start > 0) {
            snippet = "... " + snippet;
        }

        if (end < clean.length()) {
            snippet = snippet + " ...";
        }

        return snippet;
    }

    @GetMapping("/markdown")
    public Map<String, String> getMarkdown(
            @RequestParam("name") String name
    ) throws IOException {

        Path knowledgeDir = Paths.get("knowledge")
                .toAbsolutePath()
                .normalize();

        Path file = knowledgeDir
                .resolve(name)
                .normalize();

        // Prevent path traversal
        if (!file.startsWith(knowledgeDir)) {
            throw new IllegalArgumentException("Invalid file name");
        }

        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new FileNotFoundException(
                    "Markdown file not found: " + name
            );
        }

        String markdown = Files.readString(
                file,
                StandardCharsets.UTF_8
        );

        return Map.of(
                "markdownName", file.getFileName().toString(),
                "markdown", markdown
        );
    }
}