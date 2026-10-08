package com.okf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticIndex {

    private static final Path INDEX_FILE =
            Paths.get("semantic_index.json");

    private final OllamaEmbeddingService embeddingService;
    private final List<DocumentEmbedding> documentEmbeddings;
    private final ObjectMapper objectMapper;

    public SemanticIndex() {

        this.embeddingService = new OllamaEmbeddingService();
        this.documentEmbeddings = new ArrayList<>();
        this.objectMapper = new ObjectMapper();
    }

    public void buildIndex(List<Document> documents)
            throws IOException {

        documentEmbeddings.clear();

        Map<String, StoredEmbedding> oldIndex =
                loadExistingIndex();

        int reused = 0;
        int generated = 0;

        System.out.println("\nUpdating semantic index...");
        System.out.println(
                "Documents available: " + documents.size()
        );

        for (Document document : documents) {

            String documentId = document.getId();

            String currentHash =
                    calculateHash(document.getContent());

            StoredEmbedding oldEmbedding =
                    oldIndex.get(documentId);

            if (oldEmbedding != null
                    && currentHash != null
                    && currentHash.equals(oldEmbedding.hash())) {

                DocumentEmbedding documentEmbedding =
                        new DocumentEmbedding(
                                document,
                                oldEmbedding.embedding()
                        );

                documentEmbeddings.add(
                        documentEmbedding
                );

                reused++;

                System.out.println(
                        "Reusing embedding: "
                        + document.getTitle()
                );

            } else {

                System.out.println(
                        "Generating embedding: "
                        + document.getTitle()
                );

                List<Double> embedding =
                        embeddingService.getDocumentEmbedding(
                                document.getContent()
                        );

                if (embedding.isEmpty()) {

                    System.out.println(
                            "Skipping empty embedding: "
                            + document.getTitle()
                    );

                    continue;
                }

                DocumentEmbedding documentEmbedding =
                        new DocumentEmbedding(
                                document,
                                embedding
                        );

                documentEmbeddings.add(
                        documentEmbedding
                );

                generated++;

                System.out.println(
                        "  Dimensions: "
                        + embedding.size()
                );
            }
        }

        saveIndex(documents);

        System.out.println(
                "\nSemantic index updated successfully."
        );

        System.out.println(
                "Embeddings reused: " + reused
        );

        System.out.println(
                "Embeddings generated: " + generated
        );

        System.out.println(
                "Total embeddings: "
                + documentEmbeddings.size()
        );

        System.out.println(
                "Index saved to: "
                + INDEX_FILE.toAbsolutePath()
        );
    }

    private Map<String, StoredEmbedding>
    loadExistingIndex() throws IOException {

        Map<String, StoredEmbedding> index =
                new HashMap<>();

        if (!Files.exists(INDEX_FILE)) {

            System.out.println(
                    "\nNo existing semantic index found."
            );

            return index;
        }

        System.out.println(
                "\nLoading existing semantic index..."
        );

        List<StoredEmbedding> storedEmbeddings =
                objectMapper.readValue(
                        INDEX_FILE.toFile(),
                        new TypeReference<
                                List<StoredEmbedding>>() {}
                );

        for (StoredEmbedding stored :
                storedEmbeddings) {

            index.put(
                    stored.id(),
                    stored
            );
        }

        System.out.println(
                "Existing embeddings loaded: "
                + index.size()
        );

        return index;
    }

    private void saveIndex(
            List<Document> documents)
            throws IOException {

        List<StoredEmbedding> storedEmbeddings =
                new ArrayList<>();

        for (DocumentEmbedding documentEmbedding :
                documentEmbeddings) {

            Document document =
                    documentEmbedding.getDocument();

            String hash =
                    calculateHash(
                            document.getContent()
                    );

            storedEmbeddings.add(
                    new StoredEmbedding(
                            document.getId(),
                            document.getTitle(),
                            document.getContent(),
                            hash,
                            documentEmbedding.getEmbedding()
                    )
            );
        }

        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(
                        INDEX_FILE.toFile(),
                        storedEmbeddings
                );
    }

    private String calculateHash(String content) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            content.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hex =
                    new StringBuilder();

            for (byte b : hash) {

                hex.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new RuntimeException(
                    "SHA-256 algorithm not available.",
                    e
            );
        }
    }

    public List<DocumentEmbedding>
    getDocumentEmbeddings() {

        return documentEmbeddings;
    }

    private record StoredEmbedding(
            String id,
            String title,
            String content,
            String hash,
            List<Double> embedding
    ) {
    }
}