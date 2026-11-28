package com.okf;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class MarkdownLinkGenerator {

    private static final Path KNOWLEDGE_DIR =
            Paths.get("knowledge")
                    .toAbsolutePath()
                    .normalize();

    private static final String START =
            "<!-- OKF_AUTO_LINKS_START -->";

    private static final String END =
            "<!-- OKF_AUTO_LINKS_END -->";

    private static final int TOP_K = 5;

    private static final double SIMILARITY_THRESHOLD = 0.55;

    public void generateLinks(
            List<DocumentEmbedding> embeddings)
            throws IOException {

        System.out.println(
                "\nGenerating Markdown links..."
        );

        for (DocumentEmbedding source :
                embeddings) {

            List<RelatedDocument> related =
                    findRelatedDocuments(
                            source,
                            embeddings
                    );

            updateMarkdown(
                    source.getDocument().getId(),
                    related
            );
        }

        System.out.println(
                "Markdown links generated successfully."
        );
    }

    /**
     * Finds the most semantically related
     * Markdown documents.
     */
    private List<RelatedDocument>
    findRelatedDocuments(
            DocumentEmbedding source,
            List<DocumentEmbedding> embeddings) {

        List<RelatedDocument> related =
                new ArrayList<>();

        for (DocumentEmbedding target :
                embeddings) {

            /*
             * Never link a document to itself.
             */
            if (source == target) {
                continue;
            }

            /*
             * Skip embeddings with different
             * dimensions.
             */
            if (source.getEmbedding().size()
                    != target.getEmbedding().size()) {

                continue;
            }

            double similarity =
                    cosineSimilarity(
                            source.getEmbedding(),
                            target.getEmbedding()
                    );

            if (similarity
                    >= SIMILARITY_THRESHOLD) {

                related.add(
                        new RelatedDocument(
                                target.getDocument()
                                        .getId(),

                                target.getDocument()
                                        .getTitle(),

                                similarity
                        )
                );
            }
        }

        /*
         * Highest similarity first.
         */
        related.sort(
                Comparator.comparingDouble(
                        RelatedDocument::similarity
                ).reversed()
        );

        /*
         * Keep only TOP_K links.
         */
        if (related.size() > TOP_K) {

            return new ArrayList<>(
                    related.subList(
                            0,
                            TOP_K
                    )
            );
        }

        return related;
    }

    /**
     * Updates only the automatically generated
     * section of the Markdown file.
     *
     * Existing knowledge is never replaced.
     */
    private void updateMarkdown(
            String sourceName,
            List<RelatedDocument> related)
            throws IOException {

        Path sourceFile =
                resolveMarkdown(
                        sourceName
                );

        String content =
                Files.readString(
                        sourceFile,
                        StandardCharsets.UTF_8
                );

        /*
         * If there are no related documents,
         * remove an old auto-link section if
         * one exists, then leave the knowledge
         * document clean.
         */
        if (related.isEmpty()) {

            String cleaned =
                    removeAutoLinkBlock(
                            content
                    );

            if (!cleaned.equals(content)) {

                Files.writeString(
                        sourceFile,
                        cleaned,
                        StandardCharsets.UTF_8
                );
            }

            return;
        }

        StringBuilder section =
                new StringBuilder();

        section.append(START)
                .append("\n\n");

        section.append(
                "## Related Knowledge\n\n"
        );

        for (RelatedDocument document :
                related) {

            String targetName =
                    document.id();

            /*
             * Only allow safe Markdown
             * file references.
             */
            if (!isSafeTarget(
                    targetName,
                    sourceName)) {

                continue;
            }

            section.append("- [")
                    .append(
                            escapeLabel(
                                    document.title()
                            )
                    )
                    .append("](")
                    .append(targetName)
                    .append(")\n");
        }

        section.append("\n")
                .append(END);

        String updated;

        int startIndex =
                content.indexOf(
                        START
                );

        int endIndex =
                content.indexOf(
                        END
                );

        /*
         * Existing auto-link section:
         * replace ONLY that section.
         */
        if (startIndex >= 0
                && endIndex >= startIndex) {

            int afterEnd =
                    endIndex
                            + END.length();

            updated =
                    content.substring(
                            0,
                            startIndex
                    )
                    + section
                    + content.substring(
                            afterEnd
                    );

        } else {

            /*
             * No auto-link section yet.
             * Append it to the document.
             */
            updated =
                    content.stripTrailing()
                            + "\n\n"
                            + section
                            + "\n";
        }

        Files.writeString(
                sourceFile,
                updated,
                StandardCharsets.UTF_8
        );
    }

    /**
     * Removes the previously generated
     * auto-link section.
     */
    private String removeAutoLinkBlock(
            String content) {

        int startIndex =
                content.indexOf(
                        START
                );

        if (startIndex == -1) {
            return content;
        }

        int endIndex =
                content.indexOf(
                        END,
                        startIndex
                );

        if (endIndex == -1) {
            return content;
        }

        int afterEnd =
                endIndex
                        + END.length();

        return (
                content.substring(
                        0,
                        startIndex
                ).stripTrailing()
                + "\n"
                + content.substring(
                        afterEnd
                ).stripLeading()
        );
    }

    /**
     * Prevents unsafe Markdown targets.
     */
    private boolean isSafeTarget(
            String targetName,
            String sourceName) {

        if (targetName == null
                || targetName.isBlank()) {

            return false;
        }

        String lower =
                targetName.toLowerCase(
                        Locale.ROOT
                );

        if (!lower.endsWith(".md")) {
            return false;
        }

        if (targetName.contains("/")
                || targetName.contains("\\")
                || targetName.contains("..")) {

            return false;
        }

        return !targetName.equals(
                sourceName
        );
    }

    /**
     * Resolves a Markdown file inside
     * the knowledge directory.
     */
    private Path resolveMarkdown(
            String name)
            throws IOException {

        if (name == null
                || name.isBlank()) {

            throw new IOException(
                    "Empty Markdown document ID."
            );
        }

        Path file =
                KNOWLEDGE_DIR
                        .resolve(name)
                        .normalize();

        if (!file.startsWith(
                KNOWLEDGE_DIR
        )
                || !name
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .endsWith(".md")
                || !Files.isRegularFile(
                        file
                )) {

            throw new IOException(
                    "Could not resolve Markdown file: "
                            + name
            );
        }

        return file;
    }

    /**
     * Escapes Markdown link labels.
     */
    private String escapeLabel(
            String label) {

        return label == null
                ? "Related document"
                : label
                        .replace(
                                "[",
                                "\\["
                        )
                        .replace(
                                "]",
                                "\\]"
                        );
    }

    /**
     * Calculates cosine similarity.
     */
    private double cosineSimilarity(
            List<Double> a,
            List<Double> b) {

        double dot = 0.0;
        double magA = 0.0;
        double magB = 0.0;

        for (int i = 0;
             i < a.size();
             i++) {

            dot +=
                    a.get(i)
                            * b.get(i);

            magA +=
                    a.get(i)
                            * a.get(i);

            magB +=
                    b.get(i)
                            * b.get(i);
        }

        if (magA == 0.0
                || magB == 0.0) {

            return 0.0;
        }

        return dot /
                (
                        Math.sqrt(magA)
                                * Math.sqrt(magB)
                );
    }

    private record RelatedDocument(
            String id,
            String title,
            double similarity) {
    }
}