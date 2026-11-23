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

    private static final int MAX_SELECTED_PASSAGES = 4;

    private static final int MAX_RELATED_PASSAGES = 2;

    private static final Pattern MARKDOWN_LINK =
            Pattern.compile("\\[[^\\]]+\\]\\(([^)]+\\.md)\\)");

    public String buildContext(
            String selectedName,
            String question)
            throws IOException {

        Path selectedFile =
                resolveMarkdown(selectedName);

        List<String> questionTerms =
                new ArrayList<>(
                        new LinkedHashSet<>(
                                TextPreprocessor.process(question)
                        )
                );

        if (questionTerms.isEmpty()) {
            return "";
        }

        StringBuilder context =
                new StringBuilder();

        // ------------------------------------------------
        // 1. Search the currently selected document
        // ------------------------------------------------

        String selectedContent =
                Files.readString(
                        selectedFile,
                        StandardCharsets.UTF_8
                );

        List<String> selectedPassages =
                findRelevantPassages(
                        selectedContent,
                        questionTerms,
                        MAX_SELECTED_PASSAGES
                );

        for (String passage : selectedPassages) {

            context.append("\n\nSOURCE: ")
                    .append(selectedFile.getFileName())
                    .append("\n")
                    .append(passage);
        }

        // ------------------------------------------------
        // 2. Search documents already linked to selected
        // document
        // ------------------------------------------------

        List<String> relatedNames =
                extractGeneratedLinks(selectedContent);

        int relatedFileCount = 0;

        for (String relatedName : relatedNames) {

            if (relatedFileCount >= MAX_RELATED_FILES) {
                break;
            }

            if (relatedName.equals(
                    selectedFile.getFileName().toString())) {
                continue;
            }

            try {

                Path relatedFile =
                        resolveMarkdown(relatedName);

                String relatedContent =
                        Files.readString(
                                relatedFile,
                                StandardCharsets.UTF_8
                        );

                List<String> passages =
                        findRelevantPassages(
                                relatedContent,
                                questionTerms,
                                MAX_RELATED_PASSAGES
                        );

                if (!passages.isEmpty()) {

                    for (String passage : passages) {

                        context.append("\n\nSOURCE: ")
                                .append(
                                        relatedFile.getFileName()
                                )
                                .append("\n")
                                .append(passage);
                    }

                    relatedFileCount++;
                }

            } catch (IOException |
                     IllegalArgumentException e) {

                // Ignore invalid or missing links.
            }
        }

        System.out.println(
                "Current-document context prepared."
        );

        return context.toString().trim();
    }

    private List<String> findRelevantPassages(
            String markdown,
            List<String> questionTerms,
            int maxPassages) {

        List<ScoredPassage> passages =
                new ArrayList<>();

        Set<String> queryTerms =
                new LinkedHashSet<>(questionTerms);

        if (queryTerms.isEmpty()) {
            return Collections.emptyList();
        }

        /*
         * Require at least:
         *
         * 1 matching term for very short queries
         * 2 matching terms for larger queries
         *
         * This prevents a single common word from
         * making an unrelated paragraph look relevant.
         */
        int requiredMatches;

        if (queryTerms.size() <= 2) {
            requiredMatches = 1;
        } else {
            requiredMatches = 2;
        }

        for (String paragraph :
                markdown.split("\\R\\s*\\R")) {

            String cleaned =
                    paragraph.trim();

            if (cleaned.isBlank()
                    || cleaned.contains(START)
                    || cleaned.contains(END)) {
                continue;
            }

            List<String> paragraphTerms =
                    TextPreprocessor.process(cleaned);

            Set<String> paragraphTermSet =
                    new HashSet<>(paragraphTerms);

            int matches = 0;

            for (String term : queryTerms) {

                if (paragraphTermSet.contains(term)) {
                    matches++;
                }
            }

            if (matches < requiredMatches) {
                continue;
            }

            double coverage =
                    (double) matches /
                            queryTerms.size();

            double score =
                    matches * 10.0 +
                            coverage * 100.0;

            if (cleaned.length() >
                    MAX_PASSAGE_CHARS) {

                cleaned =
                        cleaned.substring(
                                0,
                                MAX_PASSAGE_CHARS
                        );
            }

            passages.add(
                    new ScoredPassage(
                            cleaned,
                            score
                    )
            );
        }

        passages.sort(
                Comparator.comparingDouble(
                        ScoredPassage::score
                ).reversed()
        );

        List<String> result =
                new ArrayList<>();

        for (int i = 0;
             i < Math.min(
                     maxPassages,
                     passages.size()
             );
             i++) {

            result.add(
                    passages.get(i).content()
            );
        }

        return result;
    }

    private List<String> extractGeneratedLinks(
            String markdown) {

        int start =
                markdown.indexOf(START);

        int end =
                markdown.indexOf(END);

        if (start < 0 ||
                end < start) {

            return Collections.emptyList();
        }

        String generatedSection =
                markdown.substring(
                        start + START.length(),
                        end
                );

        Matcher matcher =
                MARKDOWN_LINK.matcher(
                        generatedSection
                );

        Set<String> names =
                new LinkedHashSet<>();

        while (matcher.find()) {

            String name =
                    matcher.group(1).trim();

            if (name
                    .toLowerCase(Locale.ROOT)
                    .endsWith(".md")
                    && !name.contains("/")
                    && !name.contains("\\")
                    && !name.contains("..")
                    && !name.contains(":")) {

                names.add(name);
            }
        }

        return new ArrayList<>(names);
    }

    private Path resolveMarkdown(
            String name)
            throws IOException {

        if (name == null
                || name.isBlank()
                || !name
                .toLowerCase(Locale.ROOT)
                .endsWith(".md")
                || name.contains("/")
                || name.contains("\\")
                || name.contains("..")) {

            throw new IllegalArgumentException(
                    "Invalid Markdown filename."
            );
        }

        Path file =
                KNOWLEDGE_DIR
                        .resolve(name)
                        .normalize();

        if (!file.startsWith(
                KNOWLEDGE_DIR)
                || !Files.isRegularFile(file)) {

            throw new IOException(
                    "Markdown document not found: "
                            + name
            );
        }

        return file;
    }

    private record ScoredPassage(
            String content,
            double score) {
    }
}