package com.okf;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class ConceptSimilarityService {

    private static final double SIMILARITY_THRESHOLD = 0.90;

    private final OllamaEmbeddingService embeddingService;

    private final Path knowledgeDirectory =
            Paths.get("knowledge");

    public ConceptSimilarityService(
            OllamaEmbeddingService embeddingService) {

        this.embeddingService =
                embeddingService;
    }

    public SimilarConcept findSimilarConcept(
            ConceptDecompositionService.ConceptNote concept)
            throws Exception {

        String text =
                buildEmbeddingText(concept);

        List<Double> newEmbedding =
                embeddingService.getDocumentEmbedding(
                        text
                );

        if (newEmbedding == null
                || newEmbedding.isEmpty()) {

            System.out.println(
                    "Could not generate embedding for concept: "
                            + concept.title()
            );

            return null;
        }

        SimilarConcept bestMatch = null;

        if (!Files.exists(
                knowledgeDirectory)) {

            return null;
        }

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(
                             knowledgeDirectory,
                             "*.md")) {

            for (Path file : stream) {

                String content =
                        Files.readString(file);

                String existingText =
                        extractKnowledgeText(
                                content
                        );

                if (existingText.isBlank()) {
                    continue;
                }

                List<Double> existingEmbedding =
                        embeddingService
                                .getDocumentEmbedding(
                                        existingText
                                );

                if (existingEmbedding == null
                        || existingEmbedding.isEmpty()) {

                    continue;
                }

                double similarity =
                        cosineSimilarity(
                                newEmbedding,
                                existingEmbedding
                        );

                System.out.println(
                        "Concept similarity: "
                                + concept.title()
                                + " <-> "
                                + file.getFileName()
                                + " = "
                                + String.format(
                                        "%.4f",
                                        similarity
                                )
                );

                if (similarity >=
                        SIMILARITY_THRESHOLD) {

                    if (bestMatch == null
                            || similarity >
                               bestMatch.similarity()) {

                        bestMatch =
                                new SimilarConcept(
                                        file,
                                        similarity
                                );
                    }
                }
            }
        }

        return bestMatch;
    }

    private String buildEmbeddingText(
            ConceptDecompositionService.ConceptNote concept) {

        StringBuilder text =
                new StringBuilder();

        if (concept.title() != null) {
            text.append(
                    concept.title()
            ).append("\n");
        }

        if (concept.description() != null) {
            text.append(
                    concept.description()
            ).append("\n");
        }

        if (concept.tags() != null) {

            for (String tag :
                    concept.tags()) {

                text.append(tag)
                        .append(" ");
            }

            text.append("\n");
        }

        if (concept.content() != null) {
            text.append(
                    concept.content()
            );
        }

        return text.toString().trim();
    }

    private String extractKnowledgeText(
            String markdown) {

        String text =
                markdown;

        /*
         * Remove YAML frontmatter.
         */
        if (text.trim()
                .startsWith("---")) {

            String[] lines =
                    text.split(
                            "\\R",
                            -1
                    );

            int closingIndex = -1;

            for (int i = 1;
                 i < lines.length;
                 i++) {

                if (lines[i].trim()
                        .equals("---")) {

                    closingIndex = i;
                    break;
                }
            }

            if (closingIndex != -1) {

                StringBuilder body =
                        new StringBuilder();

                for (int i =
                             closingIndex + 1;
                     i < lines.length;
                     i++) {

                    body.append(
                            lines[i]
                    ).append("\n");
                }

                text =
                        body.toString();
            }
        }

        /*
         * Remove automatically generated
         * Markdown links.
         */
        String startMarker =
                "<!-- OKF_AUTO_LINKS_START -->";

        String endMarker =
                "<!-- OKF_AUTO_LINKS_END -->";

        int start =
                text.indexOf(
                        startMarker
                );

        if (start != -1) {

            int end =
                    text.indexOf(
                            endMarker,
                            start
                    );

            if (end != -1) {

                end +=
                        endMarker.length();

                text =
                        text.substring(
                                0,
                                start
                        )
                        +
                        text.substring(
                                end
                        );
            }
        }

        return text.trim();
    }

    private double cosineSimilarity(
            List<Double> a,
            List<Double> b) {

        if (a.size() != b.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int i = 0;
             i < a.size();
             i++) {

            double x = a.get(i);
            double y = b.get(i);

            dotProduct +=
                    x * y;

            magnitudeA +=
                    x * x;

            magnitudeB +=
                    y * y;
        }

        if (magnitudeA == 0
                || magnitudeB == 0) {

            return 0.0;
        }

        return dotProduct /
                (
                        Math.sqrt(
                                magnitudeA
                        )
                        *
                        Math.sqrt(
                                magnitudeB
                        )
                );
    }

    public record SimilarConcept(
            Path file,
            double similarity
    ) {
    }
}