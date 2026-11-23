package com.okf;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class MetadataDocumentMatcher {

    private final Path knowledgeDirectory =
            Paths.get("knowledge");

    public String findBestDocument(
            String query,
            String selectedDocument) throws IOException {

        String normalizedQuery =
                query.toLowerCase();

        String bestDocument = null;
        int bestScore = 0;

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(
                             knowledgeDirectory,
                             "*.md")) {

            for (Path path : stream) {

                String fileName =
                        path.getFileName().toString();

                // Never suggest the document
                // the user is already chatting with.
                if (fileName.equalsIgnoreCase(
                        selectedDocument)) {
                    continue;
                }

                String content =
                        Files.readString(path);

                Metadata metadata =
                        extractMetadata(content);

                int score =
                        calculateScore(
                                normalizedQuery,
                                metadata
                        );

                System.out.println(
                        "Metadata match: "
                                + fileName
                                + " | score="
                                + score
                );

                if (score > bestScore) {
                    bestScore = score;
                    bestDocument = fileName;
                }
            }
        }

        // Require at least one meaningful match.
        if (bestScore < 2) {
            return null;
        }

        return bestDocument;
    }

    private int calculateScore(
            String query,
            Metadata metadata) {

        int score = 0;

        Set<String> queryWords =
                tokenize(query);

        // Title match = strongest signal
        for (String word : queryWords) {
            if (metadata.title.contains(word)) {
                score += 4;
            }
        }

        // Description match
        for (String word : queryWords) {
            if (metadata.description.contains(word)) {
                score += 2;
            }
        }

        // Tag match
        for (String tag : metadata.tags) {

            for (String word : queryWords) {

                if (tag.equals(word)) {
                    score += 3;
                }
            }
        }

        // Resource match
        for (String word : queryWords) {
            if (metadata.resource.contains(word)) {
                score += 2;
            }
        }

        return score;
    }

    private Set<String> tokenize(
            String text) {

        Set<String> words =
                new HashSet<>();

        for (String word :
                text.toLowerCase()
                        .replaceAll(
                                "[^a-z0-9 ]",
                                " ")
                        .split("\\s+")) {

            if (word.length() >= 3) {
                words.add(word);
            }
        }

        return words;
    }

    private Metadata extractMetadata(
            String content) {

        Metadata metadata =
                new Metadata();

        String[] lines =
                content.split("\\R");

        boolean insideYaml = false;

        for (String line : lines) {

            String trimmed =
                    line.trim();

            if (trimmed.equals("---")) {

                if (!insideYaml) {
                    insideYaml = true;
                } else {
                    break;
                }

                continue;
            }

            if (!insideYaml) {
                continue;
            }

            if (trimmed.startsWith("title:")) {

                metadata.title =
                        cleanValue(
                                trimmed.substring(6));

            } else if (
                    trimmed.startsWith(
                            "description:")) {

                metadata.description =
                        cleanValue(
                                trimmed.substring(
                                        12));

            } else if (
                    trimmed.startsWith(
                            "resource:")) {

                metadata.resource =
                        cleanValue(
                                trimmed.substring(
                                        9));

            } else if (
                    trimmed.startsWith("- ")) {

                metadata.tags.add(
                        cleanValue(
                                trimmed.substring(2)));
            }
        }

        metadata.title =
                metadata.title.toLowerCase();

        metadata.description =
                metadata.description.toLowerCase();

        metadata.resource =
                metadata.resource.toLowerCase();

        for (int i = 0;
             i < metadata.tags.size();
             i++) {

            metadata.tags.set(
                    i,
                    metadata.tags.get(i)
                            .toLowerCase());
        }

        return metadata;
    }

    private String cleanValue(
            String value) {

        return value
                .trim()
                .replace("\"", "")
                .replace("'", "");
    }

    private static class Metadata {

        String title = "";
        String description = "";
        String resource = "";
        List<String> tags =
                new ArrayList<>();
    }
}