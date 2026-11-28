package com.okf;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ConceptMergeService {

    private static final double SIMILARITY_THRESHOLD = 0.90;

    private final Path knowledgeDirectory =
            Path.of("knowledge");

    private final ConceptSimilarityService
            conceptSimilarityService;

    public ConceptMergeService(
            ConceptSimilarityService conceptSimilarityService) {

        this.conceptSimilarityService =
                conceptSimilarityService;
    }

    /**
     * Saves a new concept or merges it into
     * an existing concept.
     *
     * Decision order:
     *
     * 1. Same resource -> merge
     * 2. Different resource -> embedding similarity
     * 3. Similarity >= 0.90 -> merge
     * 4. Otherwise -> create new file
     */
    public Path saveOrMerge(
            ConceptDecompositionService.ConceptNote concept)
            throws Exception {

        Files.createDirectories(
                knowledgeDirectory
        );

        String resource =
                sanitizeResource(
                        concept.resource()
                );

        if (resource.isBlank()) {

            throw new IOException(
                    "Concept has an invalid resource name."
            );
        }

        Path resourceFile =
                knowledgeDirectory.resolve(
                        resource + ".md"
                );

        /*
         * -------------------------------------------------
         * CASE 1: SAME RESOURCE
         * -------------------------------------------------
         */

        if (Files.exists(resourceFile)) {

            System.out.println(
                    "Existing concept found by resource: "
                            + resourceFile.getFileName()
            );

            mergeIntoExisting(
                    resourceFile,
                    concept
            );

            return resourceFile;
        }

        /*
         * -------------------------------------------------
         * CASE 2: DIFFERENT RESOURCE
         * -------------------------------------------------
         */

        System.out.println(
                "No exact resource match found for: "
                        + resource
        );

        System.out.println(
                "Checking embedding similarity..."
        );

        ConceptSimilarityService.SimilarConcept
                similarConcept =
                conceptSimilarityService
                        .findSimilarConcept(
                                concept
                        );

        /*
         * -------------------------------------------------
         * CASE 3: SEMANTIC MATCH
         * -------------------------------------------------
         */

        if (similarConcept != null
                && similarConcept.similarity()
                    >= SIMILARITY_THRESHOLD) {

            System.out.println(
                    "Semantic duplicate detected."
            );

            System.out.println(
                    "Existing file: "
                            + similarConcept.file()
                                    .getFileName()
            );

            System.out.println(
                    "Similarity: "
                            + String.format(
                                    "%.4f",
                                    similarConcept
                                            .similarity()
                            )
            );

            mergeIntoExisting(
                    similarConcept.file(),
                    concept
            );

            return similarConcept.file();
        }

        /*
         * -------------------------------------------------
         * CASE 4: COMPLETELY NEW CONCEPT
         * -------------------------------------------------
         */

        System.out.println(
                "No similar concept found."
        );

        System.out.println(
                "Creating new concept: "
                        + resource
        );

        writeNewConcept(
                resourceFile,
                concept
        );

        return resourceFile;
    }

    /**
     * Creates a completely new Markdown file.
     */
    private void writeNewConcept(
            Path file,
            ConceptDecompositionService.ConceptNote concept)
            throws IOException {

        String markdown =
                buildMarkdown(
                        concept
                );

        Files.writeString(
                file,
                markdown,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        System.out.println(
                "New concept Markdown created: "
                        + file.getFileName()
        );
    }

    /**
     * Merges new knowledge into an existing file.
     *
     * Existing knowledge is preserved.
     */
    private void mergeIntoExisting(
            Path file,
            ConceptDecompositionService.ConceptNote concept)
            throws IOException {

        String existing =
                Files.readString(
                        file,
                        StandardCharsets.UTF_8
                );

        String newContent =
                concept.content();

        if (newContent == null
                || newContent.isBlank()) {

            System.out.println(
                    "New concept has no content. "
                            + "Merge skipped."
            );

            return;
        }

        newContent =
                removeYamlFrontMatter(
                        newContent
                ).trim();

        if (newContent.isBlank()) {

            System.out.println(
                    "New concept content is empty. "
                            + "Merge skipped."
            );

            return;
        }

        /*
         * Prevent the same knowledge from being
         * appended repeatedly.
         */
        if (containsNormalizedContent(
                existing,
                newContent)) {

            System.out.println(
                    "Content already exists. "
                            + "Merge skipped."
            );

            return;
        }

        /*
         * Remove generated links before modifying
         * the document. They will be regenerated
         * by MarkdownLinkGenerator.
         */
        String existingWithoutLinks =
                removeAutoLinkBlock(
                        existing
                );

        String merged =
                existingWithoutLinks.trim()
                        + "\n\n"
                        + newContent
                        + "\n";

        Files.writeString(
                file,
                merged,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        System.out.println(
                "New knowledge merged into: "
                        + file.getFileName()
        );
    }

    /**
     * Creates YAML frontmatter + Markdown content.
     */
    private String buildMarkdown(
            ConceptDecompositionService.ConceptNote concept) {

        StringBuilder markdown =
                new StringBuilder();

        markdown.append("---\n");

        markdown.append("type: document\n");

        markdown.append("title: ")
                .append(
                        quote(
                                concept.title()
                        )
                )
                .append("\n");

        markdown.append("description: ")
                .append(
                        quote(
                                concept.description()
                        )
                )
                .append("\n");

        markdown.append("tags:\n");

        List<String> tags =
                concept.tags();

        if (tags != null) {

            for (String tag :
                    tags) {

                if (tag != null
                        && !tag.isBlank()) {

                    markdown.append("  - ")
                            .append(
                                    quote(tag)
                            )
                            .append("\n");
                }
            }
        }

        markdown.append("resource: ")
                .append(
                        quote(
                                sanitizeResource(
                                        concept.resource()
                                )
                        )
                )
                .append("\n");

        markdown.append("timestamp: ")
                .append(
                        quote(
                                LocalDate.now()
                                        .toString()
                        )
                )
                .append("\n");

        markdown.append("---\n\n");

        String content =
                concept.content();

        if (content != null
                && !content.isBlank()) {

            markdown.append(
                    removeYamlFrontMatter(
                            content
                    )
            );
        }

        return markdown.toString();
    }

    /**
     * Removes YAML frontmatter if Ollama accidentally
     * included it in the generated content.
     */
    private String removeYamlFrontMatter(
            String content) {

        String normalized =
                content.trim();

        if (!normalized.startsWith("---")) {
            return normalized;
        }

        String[] lines =
                normalized.split(
                        "\\R",
                        -1
                );

        int closingIndex = -1;

        for (int i = 1;
             i < lines.length;
             i++) {

            String line =
                    lines[i].trim();

            if (line.equals("---")
                    || line.equals("...")) {

                closingIndex = i;
                break;
            }
        }

        if (closingIndex == -1) {
            return normalized;
        }

        StringBuilder result =
                new StringBuilder();

        for (int i = closingIndex + 1;
             i < lines.length;
             i++) {

            result.append(
                    lines[i]
            );

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString().trim();
    }

    /**
     * Removes generated automatic Markdown links.
     */
    private String removeAutoLinkBlock(
            String content) {

        String startMarker =
                "<!-- OKF_AUTO_LINKS_START -->";

        String endMarker =
                "<!-- OKF_AUTO_LINKS_END -->";

        int start =
                content.indexOf(
                        startMarker
                );

        if (start == -1) {
            return content;
        }

        int end =
                content.indexOf(
                        endMarker,
                        start
                );

        if (end == -1) {
            return content;
        }

        end +=
                endMarker.length();

        String before =
                content.substring(
                        0,
                        start
                ).stripTrailing();

        String after =
                content.substring(
                        end
                ).stripLeading();

        if (after.isEmpty()) {
            return before;
        }

        if (before.isEmpty()) {
            return after;
        }

        return before
                + "\n\n"
                + after;
    }

    /**
     * Checks whether the new content already exists
     * in the existing document.
     */
    private boolean containsNormalizedContent(
            String existing,
            String newContent) {

        String normalizedExisting =
                normalizeForComparison(
                        existing
                );

        String normalizedNew =
                normalizeForComparison(
                        newContent
                );

        return normalizedExisting.contains(
                normalizedNew
        );
    }

    private String normalizeForComparison(
            String text) {

        return text
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    /**
     * Converts a resource name into a safe
     * Markdown filename.
     */
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

    private String quote(
            String value) {

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