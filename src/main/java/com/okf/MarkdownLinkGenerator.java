
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
            Paths.get("knowledge").toAbsolutePath().normalize();

    private static final String START =
            "<!-- OKF_AUTO_LINKS_START -->";

    private static final String END =
            "<!-- OKF_AUTO_LINKS_END -->";

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.55;

    public void generateLinks(
            List<DocumentEmbedding> embeddings) throws IOException {

        System.out.println("\nGenerating Markdown links...");

        for (DocumentEmbedding source : embeddings) {

            List<RelatedDocument> related = new ArrayList<>();

            for (DocumentEmbedding target : embeddings) {

                if (source == target) {
                    continue;
                }

                if (source.getEmbedding().size()
                        != target.getEmbedding().size()) {
                    continue;
                }

                double similarity = cosineSimilarity(
                        source.getEmbedding(),
                        target.getEmbedding()
                );

                if (similarity >= SIMILARITY_THRESHOLD) {
                    related.add(new RelatedDocument(
                            target.getDocument().getId(),
                            target.getDocument().getTitle(),
                            similarity
                    ));
                }
            }

            related.sort(
                    Comparator.comparingDouble(
                            RelatedDocument::similarity
                    ).reversed()
            );

            List<RelatedDocument> topRelated =
                    related.subList(
                            0, Math.min(TOP_K, related.size()));

            updateMarkdown(source.getDocument().getId(), topRelated);
        }

        System.out.println("Markdown links generated successfully.");
    }

    private void updateMarkdown(
            String sourceName,
            List<RelatedDocument> related) throws IOException {

        Path sourceFile = resolveMarkdown(sourceName);

        String content =
                Files.readString(sourceFile, StandardCharsets.UTF_8);

        StringBuilder section = new StringBuilder();
        section.append(START).append("\n\n");
        section.append("## Related Documents\n\n");

        for (RelatedDocument document : related) {
            String targetName = document.id();

            // Do not generate unsafe or invalid links.
            if (!targetName.toLowerCase(Locale.ROOT).endsWith(".md")
                    || targetName.contains("/")
                    || targetName.contains("\\")
                    || targetName.equals(sourceName)) {
                continue;
            }

            section.append("- [")
                    .append(escapeLabel(document.title()))
                    .append("](")
                    .append(targetName)
                    .append(")\n");
        }

        section.append("\n").append(END);

        String updated;

        int startIndex = content.indexOf(START);
        int endIndex = content.indexOf(END);

        if (startIndex >= 0 && endIndex >= startIndex) {
            // Replace only the previously generated section.
            int afterEnd = endIndex + END.length();

            updated = content.substring(0, startIndex)
                    + section
                    + content.substring(afterEnd);
        } else {
            // Append the section without overwriting existing content.
            updated = content.stripTrailing()
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

    private Path resolveMarkdown(String name) throws IOException {

        if (name == null || name.isBlank()) {
            throw new IOException("Empty Markdown document ID.");
        }

        Path file = KNOWLEDGE_DIR.resolve(name).normalize();

        if (!file.startsWith(KNOWLEDGE_DIR)
                || !name.toLowerCase(Locale.ROOT).endsWith(".md")
                || !Files.isRegularFile(file)) {
            throw new IOException(
                    "Could not resolve Markdown file: " + name);
        }

        return file;
    }

    private String escapeLabel(String label) {
        return label == null ? "Related document"
                : label.replace("[", "\\[")
                       .replace("]", "\\]");
    }

    private double cosineSimilarity(
            List<Double> a, List<Double> b) {

        double dot = 0.0;
        double magA = 0.0;
        double magB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            magA += a.get(i) * a.get(i);
            magB += b.get(i) * b.get(i);
        }

        if (magA == 0.0 || magB == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(magA) * Math.sqrt(magB));
    }

    private record RelatedDocument(
            String id, String title, double similarity) {
    }
}
