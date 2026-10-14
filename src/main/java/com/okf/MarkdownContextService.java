
package com.okf;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarkdownContextService {

    private static final Path KNOWLEDGE_DIR =
            Paths.get("knowledge").toAbsolutePath().normalize();

    private static final String START =
            "<!-- OKF_AUTO_LINKS_START -->";

    private static final String END =
            "<!-- OKF_AUTO_LINKS_END -->";

    private static final int MAX_RELATED_FILES = 3;
    private static final int MAX_PASSAGE_CHARS = 1800;

    private static final Pattern MARKDOWN_LINK =
            Pattern.compile("\\[[^\\]]+\\]\\(([^)]+\\.md)\\)");

    public String buildContext(String selectedName, String question)
            throws IOException {

        Path selectedFile = resolveMarkdown(selectedName);

        String selectedContent =
                Files.readString(selectedFile, StandardCharsets.UTF_8);

        StringBuilder context = new StringBuilder();

        context.append("SELECTED DOCUMENT: ")
                .append(selectedFile.getFileName())
                .append("\n")
                .append(selectedContent);

        List<String> relatedNames = extractGeneratedLinks(selectedContent);

        List<RelatedPassage> passages = new ArrayList<>();

        for (String relatedName : relatedNames) {
            if (passages.size() >= MAX_RELATED_FILES) {
                break;
            }

            // Do not include the selected file again.
            if (relatedName.equals(selectedFile.getFileName().toString())) {
                continue;
            }

            try {
                Path relatedFile = resolveMarkdown(relatedName);

                String relatedContent =
                        Files.readString(
                                relatedFile, StandardCharsets.UTF_8);

                String passage =
                        findRelevantPassage(relatedContent, question);

                if (!passage.isBlank()) {
                    passages.add(new RelatedPassage(
                            relatedFile.getFileName().toString(),
                            passage
                    ));
                }
            } catch (IOException | IllegalArgumentException e) {
                // Skip missing or invalid links; keep chat available.
            }
        }

        for (RelatedPassage passage : passages) {
            context.append("\n\nRELATED DOCUMENT: ")
                    .append(passage.name())
                    .append("\n")
                    .append(passage.content());
        }

        System.out.println(
                "Markdown context prepared. Related passages: "
                        + passages.size()
        );

        return context.toString();
    }

    private List<String> extractGeneratedLinks(String markdown) {

        int start = markdown.indexOf(START);
        int end = markdown.indexOf(END);

        if (start < 0 || end < start) {
            return Collections.emptyList();
        }

        String generatedSection =
                markdown.substring(start + START.length(), end);

        Matcher matcher = MARKDOWN_LINK.matcher(generatedSection);

        Set<String> names = new LinkedHashSet<>();

        while (matcher.find()) {
            String name = matcher.group(1).trim();

            // Generated links should be simple filenames, not paths or URLs.
            if (name.toLowerCase(Locale.ROOT).endsWith(".md")
                    && !name.contains("/")
                    && !name.contains("\\")
                    && !name.contains("..")
                    && !name.contains(":")) {
                names.add(name);
            }
        }

        return new ArrayList<>(names);
    }

    private Path resolveMarkdown(String name) throws IOException {

        if (name == null || name.isBlank()
                || !name.toLowerCase(Locale.ROOT).endsWith(".md")
                || name.contains("/") || name.contains("\\")
                || name.contains("..")) {
            throw new IllegalArgumentException(
                    "Invalid Markdown filename.");
        }

        Path file = KNOWLEDGE_DIR.resolve(name).normalize();

        if (!file.startsWith(KNOWLEDGE_DIR)
                || !Files.isRegularFile(file)) {
            throw new IOException(
                    "Markdown document not found: " + name);
        }

        return file;
    }

    private String findRelevantPassage(
            String markdown, String question) {

        List<String> questionTerms = TextPreprocessor.process(question);

        if (questionTerms.isEmpty()) {
            return "";
        }

        String bestParagraph = "";
        int bestScore = 0;

        // Consider paragraphs separated by blank lines.
        for (String paragraph : markdown.split("\\R\\s*\\R")) {

            String cleaned = paragraph.trim();

            if (cleaned.isBlank()
                    || cleaned.contains(START)
                    || cleaned.contains(END)) {
                continue;
            }

            List<String> paragraphTerms =
                    TextPreprocessor.process(cleaned);

            Set<String> paragraphTermSet =
                    new HashSet<>(paragraphTerms);

            int score = 0;

            for (String term : questionTerms) {
                if (paragraphTermSet.contains(term)) {
                    score++;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestParagraph = cleaned;
            }
        }

        if (bestParagraph.length() > MAX_PASSAGE_CHARS) {
            bestParagraph =
                    bestParagraph.substring(0, MAX_PASSAGE_CHARS);
        }

        // Don't add a related passage with no matching query terms.
        return bestScore > 0 ? bestParagraph : "";
    }

    private record RelatedPassage(String name, String content) {
    }
}
