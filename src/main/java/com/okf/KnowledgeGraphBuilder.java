
package com.okf;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

public class KnowledgeGraphBuilder {

    private static final Path GRAPH_FILE =
            Paths.get("knowledge_graph.json");

    // Keep at most 5 nearest neighbors per document.
    private static final int TOP_K = 5;

    // Starter threshold; tune after inspecting real relationships.
    private static final double SIMILARITY_THRESHOLD = 0.55;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void buildGraph(
            List<DocumentEmbedding> documentEmbeddings)
            throws IOException {

        int n = documentEmbeddings.size();

        System.out.println("\nBuilding automatic knowledge graph...");
        System.out.println("Documents: " + n);

        // Each document selects its own top-K neighbors.
        Map<String, List<Candidate>> nearestNeighbors =
                new HashMap<>();

        for (int i = 0; i < n; i++) {

            DocumentEmbedding current = documentEmbeddings.get(i);
            List<Candidate> candidates = new ArrayList<>();

            for (int j = 0; j < n; j++) {

                if (i == j) {
                    continue;
                }

                DocumentEmbedding other = documentEmbeddings.get(j);

                if (current.getEmbedding().size()
                        != other.getEmbedding().size()) {
                    continue;
                }

                double score = cosineSimilarity(
                        current.getEmbedding(),
                        other.getEmbedding()
                );

                if (score >= SIMILARITY_THRESHOLD) {
                    candidates.add(
                            new Candidate(
                                    other.getDocument().getId(),
                                    other.getDocument().getTitle(),
                                    score
                            )
                    );
                }
            }

            candidates.sort(
                    Comparator.comparingDouble(Candidate::score)
                            .reversed()
            );

            List<Candidate> topCandidates =
                    new ArrayList<>(
                            candidates.subList(
                                    0,
                                    Math.min(TOP_K, candidates.size())
                            )
                    );

            nearestNeighbors.put(
                    current.getDocument().getId(),
                    topCandidates
            );
        }

        /*
         * Make relationships undirected:
         * if A selects B, retain A-B even if B doesn't select A.
         * The same relationship is stored only once.
         */
        Map<String, GraphEdge> uniqueEdges = new LinkedHashMap<>();

        for (DocumentEmbedding sourceEmbedding : documentEmbeddings) {

            String sourceId =
                    sourceEmbedding.getDocument().getId();

            for (Candidate candidate :
                    nearestNeighbors.getOrDefault(
                            sourceId, Collections.emptyList())) {

                String targetId = candidate.id();

                String first = sourceId.compareTo(targetId) <= 0
                        ? sourceId : targetId;

                String second = sourceId.compareTo(targetId) <= 0
                        ? targetId : sourceId;

                String edgeKey = first + "\u0000" + second;

                uniqueEdges.putIfAbsent(
                        edgeKey,
                        new GraphEdge(
                                first,
                                second,
                                candidate.score(),
                                "semantic_similarity",
                                "auto"
                        )
                );
            }
        }

        GraphData graph = new GraphData(
                1,
                Instant.now().toString(),
                n,
                uniqueEdges.size(),
                new ArrayList<>(uniqueEdges.values())
        );

        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(GRAPH_FILE.toFile(), graph);

        System.out.println("Knowledge graph generated.");
        System.out.println("Documents: " + n);
        System.out.println("Relationships: " + uniqueEdges.size());
        System.out.println(
                "Saved to: " + GRAPH_FILE.toAbsolutePath()
        );
    }

    private double cosineSimilarity(
            List<Double> a,
            List<Double> b) {

        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int i = 0; i < a.size(); i++) {

            double x = a.get(i);
            double y = b.get(i);

            dotProduct += x * y;
            magnitudeA += x * x;
            magnitudeB += y * y;
        }

        if (magnitudeA == 0.0 || magnitudeB == 0.0) {
            return 0.0;
        }

        return dotProduct /
                (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB));
    }

    private record Candidate(
            String id,
            String title,
            double score) {
    }

    private record GraphEdge(
            String source,
            String target,
            double score,
            String relationship,
            String origin) {
    }

    private record GraphData(
            int version,
            String generatedAt,
            int documentCount,
            int relationshipCount,
            List<GraphEdge> edges) {
    }
}
