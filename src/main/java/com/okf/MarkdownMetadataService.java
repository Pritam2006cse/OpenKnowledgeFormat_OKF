
package com.okf;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarkdownMetadataService {

    private static final Set<String> STOP_WORDS = Set.of(
        "the", "and", "for", "are", "with", "this", "that",
        "from", "was", "were", "have", "has", "had", "will",
        "can", "you", "your", "into", "its", "our", "not",
        "but", "about", "there", "their", "they", "them",
        "then", "than", "when", "where", "which", "what",
        "page", "pages", "document"
    );

    public void generateMetadataForAll(String directoryPath)
            throws IOException {

        Path directory = Paths.get(directoryPath)
                .toAbsolutePath()
                .normalize();

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            return;
        }

        try (DirectoryStream<Path> files =
                     Files.newDirectoryStream(directory, "*.md")) {

            for (Path file : files) {
                addMetadata(file);
            }
        }
    }

    private void addMetadata(Path file) throws IOException {

        String content = Files.readString(
                file, StandardCharsets.UTF_8);

        // Preserve existing YAML front matter.
        if (hasYamlFrontMatter(content)) {
            System.out.println(
                    "Metadata already exists: " + file.getFileName());
            return;
        }

        String title = extractTitle(content, file);
        String description = extractDescription(content);
        List<String> tags = extractTags(title + " " + description);
        String resource = createResource(file.getFileName().toString());
        String timestamp = LocalDate.now().toString();
        String fileSize = getFileSize(file);

        StringBuilder yaml = new StringBuilder();

        yaml.append("---\n");
        yaml.append("type: document\n");
        yaml.append("title: ").append(quote(title)).append("\n");
        yaml.append("description: ").append(quote(description)).append("\n");
        yaml.append("tags:\n");

        if (tags.isEmpty()) {
            yaml.append("tags: []\n");
        } else {
            yaml.append("tags:\n");
            for (String tag : tags) {
                yaml.append("  - ").append(quote(tag)).append("\n");
            }
        }

        yaml.append("resource: ").append(quote(resource)).append("\n");
        yaml.append("timestamp: ").append(quote(timestamp)).append("\n");
        yaml.append("file_size: ").append(quote(fileSize)).append("\n");
        yaml.append("---\n\n");

        Files.writeString(
                file,
                yaml + content,
                StandardCharsets.UTF_8
        );

        System.out.println(
                "YAML metadata added: " + file.getFileName());
    }

    private boolean hasYamlFrontMatter(String content) {

        String normalized = content.startsWith("\uFEFF")
                ? content.substring(1)
                : content;

        if (!normalized.startsWith("---")) {
            return false;
        }

        String[] lines = normalized.split("\\R", -1);

        if (lines.length == 0 || !lines[0].trim().equals("---")) {
            return false;
        }

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().equals("---")
                    || lines[i].trim().equals("...")) {
                return true;
            }
        }

        return false;
    }

    private String extractTitle(String content, Path file) {

        Matcher matcher = Pattern.compile(
                "(?m)^#\\s+(.+?)\\s*$"
        ).matcher(content);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        String filename = file.getFileName().toString();

        return filename.replaceFirst("(?i)\\.md$", "")
                .replaceAll("[_-]+", " ")
                .trim();
    }

    private String extractDescription(String content) {

        StringBuilder text = new StringBuilder();

        for (String line : content.split("\\R")) {

            String trimmed = line.trim();

            if (trimmed.isEmpty()
                    || trimmed.startsWith("#")
                    || trimmed.startsWith("---")) {
                continue;
            }

            text.append(trimmed).append(" ");

            if (text.length() >= 250) {
                break;
            }
        }

        String description = text.toString()
                .replaceAll("!\\[([^]]*)]\\([^)]*\\)", "$1")
                .replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1")
                .replaceAll("[*_`>]", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (description.length() > 200) {
            description = description.substring(0, 197) + "...";
        }

        if (description.isEmpty()) {
            return "Markdown knowledge document.";
        }

        return description;
    }

    private List<String> extractTags(String text) {

        Matcher matcher = Pattern.compile("[a-zA-Z][a-zA-Z0-9-]{2,}")
                .matcher(text.toLowerCase(Locale.ROOT));

        Map<String, Integer> frequencies = new HashMap<>();

        while (matcher.find()) {
            String word = matcher.group();

            if (!STOP_WORDS.contains(word)) {
                frequencies.merge(word, 1, Integer::sum);
            }
        }

        return frequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue()
                        .reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(8)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String createResource(String filename) {

        return filename.replaceFirst("(?i)\\.md$", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String quote(String value) {

        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ") + "\"";
    }
    private String getFileSize(Path filePath) throws IOException {
        long bytes = Files.size(filePath);

        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB",
                    bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}