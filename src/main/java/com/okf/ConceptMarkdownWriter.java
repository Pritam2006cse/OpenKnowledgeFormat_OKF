package com.okf;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ConceptMarkdownWriter {

    private final Path knowledgeDirectory =
            Path.of("knowledge");

    public Path writeConcept(
            ConceptDecompositionService.ConceptNote concept)
            throws IOException {

        Files.createDirectories(knowledgeDirectory);

        String resource =
                sanitizeResource(concept.resource());

        if (resource.isBlank()) {
            throw new IOException(
                    "Concept has an invalid resource name."
            );
        }

        Path outputFile =
                knowledgeDirectory.resolve(
                        resource + ".md"
                );

        String markdown =
                buildMarkdown(concept);

        Files.writeString(
                outputFile,
                markdown,
                StandardCharsets.UTF_8
        );

        System.out.println(
                "Concept Markdown created: "
                        + outputFile
        );

        return outputFile;
    }

    private String buildMarkdown(
            ConceptDecompositionService.ConceptNote concept) {

        StringBuilder markdown =
                new StringBuilder();

        markdown.append("---\n");

        markdown.append("type: document\n");

        markdown.append("title: ")
                .append(quote(concept.title()))
                .append("\n");

        markdown.append("description: ")
                .append(quote(concept.description()))
                .append("\n");

        markdown.append("tags:\n");

        List<String> tags = concept.tags();

        if (tags != null) {

            for (String tag : tags) {

                if (tag != null
                        && !tag.isBlank()) {

                    markdown.append("  - ")
                            .append(quote(tag))
                            .append("\n");
                }
            }
        }

        markdown.append("resource: ")
                .append(quote(
                        sanitizeResource(
                                concept.resource())))
                .append("\n");

        markdown.append("timestamp: ")
                .append(quote(
                        LocalDate.now().toString()))
                .append("\n");

        markdown.append("---\n\n");

        String content =
                concept.content();

        if (content != null
                && !content.isBlank()) {

            markdown.append(
                    removeYamlFrontMatter(content)
            );
        }

        return markdown.toString();
    }

    private String removeYamlFrontMatter(
            String content) {

        String normalized =
                content.trim();

        if (!normalized.startsWith("---")) {
            return normalized + "\n";
        }

        String[] lines =
                normalized.split("\\R", -1);

        int closingIndex = -1;

        for (int i = 1;
             i < lines.length;
             i++) {

            if (lines[i].trim().equals("---")) {
                closingIndex = i;
                break;
            }
        }

        if (closingIndex == -1) {
            return normalized + "\n";
        }

        StringBuilder result =
                new StringBuilder();

        for (int i = closingIndex + 1;
             i < lines.length;
             i++) {

            result.append(lines[i]);

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString().trim() + "\n";
    }

    private String sanitizeResource(
            String resource) {

        if (resource == null) {
            return "";
        }

        return resource
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "[^a-z0-9_]+",
                        "_"
                )
                .replaceAll(
                        "_+",
                        "_"
                )
                .replaceAll(
                        "^_+|_+$",
                        "");
    }

    private String quote(String value) {

        if (value == null) {
            return "\"\"";
        }

        return "\""
                + value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", " ")
                + "\"";
    }
}