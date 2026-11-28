package com.okf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SemanticIndex {

    private static final String DATABASE_URL =
            "jdbc:sqlite:okf.db";

    private static final String AUTO_LINK_START =
            "<!-- OKF_AUTO_LINKS_START -->";

    private static final String AUTO_LINK_END =
            "<!-- OKF_AUTO_LINKS_END -->";

    private final OllamaEmbeddingService embeddingService;
    private final List<DocumentEmbedding> documentEmbeddings;

    public SemanticIndex() {

        this.embeddingService =
                new OllamaEmbeddingService();

        this.documentEmbeddings =
                new ArrayList<>();

        initializeDatabase();
    }

    /**
     * Creates the SQLite database and table
     * if they do not already exist.
     */
    private void initializeDatabase() {

        String semanticTableSql = """
                CREATE TABLE IF NOT EXISTS semantic_documents (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT,
                tags TEXT,
                hash TEXT NOT NULL,
                embedding BLOB NOT NULL
                )
                """;

        String ftsTableSql = """
                CREATE VIRTUAL TABLE IF NOT EXISTS semantic_documents_fts
                USING fts5(
                id UNINDEXED,
                title,
                description,
                tags
                )
                """;

        try (Connection connection =
                DriverManager.getConnection(DATABASE_URL)) {

        try (PreparedStatement statement =
                connection.prepareStatement(semanticTableSql)) {

                statement.executeUpdate();
        }

        try (PreparedStatement statement =
                connection.prepareStatement(ftsTableSql)) {

                statement.executeUpdate();
        }

        System.out.println(
                "SQLite semantic index initialized."
        );

        } catch (SQLException e) {

        throw new RuntimeException(
                "Failed to initialize SQLite semantic index.",
                e
        );
        }
    }

    /**
     * Builds the semantic index.
     *
     * Existing embeddings are reused when the
     * semantic Markdown content has not changed.
     */
    public void buildIndex(
            List<Document> documents)
            throws IOException {

        documentEmbeddings.clear();

        int reused = 0;
        int generated = 0;

        System.out.println(
                "\nUpdating semantic index..."
        );

        System.out.println(
                "Documents available: "
                        + documents.size()
        );

        for (Document document :
                documents) {

            String documentId =
                    document.getId();

            /*
             * Hash only the actual knowledge.
             *
             * Generated OKF links are excluded.
             */
            String semanticContent =
                    removeAutoLinkBlock(
                            document.getContent()
                    );

            String currentHash =
                    calculateHash(
                            semanticContent
                    );

            StoredEmbedding oldEmbedding =
                    findEmbedding(documentId);

            /*
             * ------------------------------------------------
             * REUSE EXISTING EMBEDDING
             * ------------------------------------------------
             */

            if (oldEmbedding != null
                    && currentHash != null
                    && currentHash.equals(
                            oldEmbedding.hash()
                    )) {

                DocumentEmbedding documentEmbedding =
                        new DocumentEmbedding(
                                document,
                                oldEmbedding.embedding()
                        );

                documentEmbeddings.add(
                        documentEmbedding
                );
                updateFtsIndex(
                        document
                );

                reused++;

                System.out.println(
                        "Reusing embedding: "
                                + document.getTitle()
                );
            }

            /*
             * ------------------------------------------------
             * GENERATE NEW EMBEDDING
             * ------------------------------------------------
             */

            else {

                System.out.println(
                        "Generating embedding: "
                                + document.getTitle()
                );

                /*
                 * Generate the embedding from the
                 * actual knowledge only.
                 *
                 * Auto-generated links are excluded.
                 */
                List<Double> embedding =
                        embeddingService
                                .getDocumentEmbedding(
                                        semanticContent
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

                saveEmbedding(
                        document,
                        currentHash,
                        embedding
                );

                generated++;

                System.out.println(
                        "  Dimensions: "
                                + embedding.size()
                );
            }
        }

        /*
         * Remove database entries for documents
         * that no longer exist in knowledge/.
         */
        removeDeletedDocuments(documents);

        System.out.println(
                "\nSemantic index updated successfully."
        );

        System.out.println(
                "Embeddings reused: "
                        + reused
        );

        System.out.println(
                "Embeddings generated: "
                        + generated
        );

        System.out.println(
                "Total embeddings: "
                        + documentEmbeddings.size()
        );

        System.out.println(
                "Index stored in SQLite: "
                        + Paths.get("okf.db")
                                .toAbsolutePath()
        );
    }

    /**
     * Finds an existing embedding by document ID.
     */
    private StoredEmbedding findEmbedding(
            String documentId) {

        String sql = """
                SELECT id, title, hash, embedding
                FROM semantic_documents
                WHERE id = ?
                """;

        try (Connection connection =
                     DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    documentId
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                String id =
                        resultSet.getString("id");

                String title =
                        resultSet.getString("title");

                String hash =
                        resultSet.getString("hash");

                byte[] embeddingBytes =
                        resultSet.getBytes("embedding");

                List<Double> embedding =
                        bytesToEmbedding(
                                embeddingBytes
                        );

                return new StoredEmbedding(
                        id,
                        title,
                        hash,
                        embedding
                );
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to load embedding from SQLite.",
                    e
            );
        }
    }

    /**
     * Saves or replaces one document embedding.
     */
    private void saveEmbedding(
            Document document,
            String hash,
            List<Double> embedding) {

        String sql = """
                INSERT INTO semantic_documents
                    (id, title, description, tags, hash, embedding)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id)
                DO UPDATE SET
                    title = excluded.title,
                    description = excluded.description,
                    tags = excluded.tags,
                    hash = excluded.hash,
                    embedding = excluded.embedding
                """;

        String description =
                extractMetadata(
                        document.getContent(),
                        "description"
                );

        String tags =
                extractTags(
                        document.getContent()
                );

        byte[] embeddingBytes =
                embeddingToBytes(
                        embedding
                );

        try (Connection connection =
                     DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    document.getId()
            );

            statement.setString(
                    2,
                    document.getTitle()
            );

            statement.setString(
                    3,
                    description
            );

            statement.setString(
                    4,
                    tags
            );

            statement.setString(
                    5,
                    hash
            );

            statement.setBytes(
                    6,
                    embeddingBytes
            );

            statement.executeUpdate();
            updateFtsIndex(
                        document
                );

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to save embedding to SQLite.",
                    e
            );
        }
    }

    /**
     * Deletes database entries for documents
     * that are no longer present in knowledge/.
     */
    private void removeDeletedDocuments(
            List<Document> documents) {

        List<String> currentIds =
                documents.stream()
                        .map(Document::getId)
                        .toList();

        String selectSql =
                "SELECT id FROM semantic_documents";

        try (Connection connection =
                     DriverManager.getConnection(DATABASE_URL);
             PreparedStatement selectStatement =
                     connection.prepareStatement(selectSql);
             ResultSet resultSet =
                     selectStatement.executeQuery()) {

            List<String> idsToDelete =
                    new ArrayList<>();

            while (resultSet.next()) {

                String id =
                        resultSet.getString("id");

                if (!currentIds.contains(id)) {

                    idsToDelete.add(id);
                }
            }

            String deleteSql =
                    "DELETE FROM semantic_documents WHERE id = ?";

            try (PreparedStatement deleteStatement =
                         connection.prepareStatement(deleteSql)) {

                for (String id :
                        idsToDelete) {

                    deleteStatement.setString(
                            1,
                            id
                    );

                    deleteStatement.executeUpdate();
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to remove deleted semantic documents.",
                    e
            );
        }
    }

    private void updateFtsIndex(
        Document document) {

        String deleteSql = """
                DELETE FROM semantic_documents_fts
                WHERE id = ?
                """;

        String insertSql = """
                INSERT INTO semantic_documents_fts
                        (id, title, description, tags)
                VALUES (?, ?, ?, ?)
                """;

        String description =
                extractMetadata(
                        document.getContent(),
                        "description"
                );

        String tags =
                extractTags(
                        document.getContent()
                );

        try (Connection connection =
                        DriverManager.getConnection(
                                DATABASE_URL
                        )) {

                try (PreparedStatement deleteStatement =
                        connection.prepareStatement(
                                deleteSql
                        )) {

                deleteStatement.setString(
                        1,
                        document.getId()
                );

                deleteStatement.executeUpdate();
                }

                try (PreparedStatement insertStatement =
                        connection.prepareStatement(
                                insertSql
                        )) {

                insertStatement.setString(
                        1,
                        document.getId()
                );

                insertStatement.setString(
                        2,
                        document.getTitle()
                );

                insertStatement.setString(
                        3,
                        description
                );

                insertStatement.setString(
                        4,
                        tags
                );

                insertStatement.executeUpdate();
                }

        } catch (SQLException e) {

                throw new RuntimeException(
                        "Failed to update FTS5 index.",
                        e
                );
        }
        }

    /**
     * Converts embedding List<Double> into
     * compact binary data for SQLite.
     */
    private byte[] embeddingToBytes(
            List<Double> embedding) {

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        embedding.size() * Double.BYTES
                ).order(
                        ByteOrder.LITTLE_ENDIAN
                );

        for (Double value :
                embedding) {

            buffer.putDouble(value);
        }

        return buffer.array();
    }

    /**
     * Converts SQLite binary embedding
     * back into List<Double>.
     */
    private List<Double> bytesToEmbedding(
            byte[] bytes) {

        if (bytes == null
                || bytes.length == 0) {

            return List.of();
        }

        if (bytes.length % Double.BYTES != 0) {

            throw new IllegalArgumentException(
                    "Invalid embedding byte length."
            );
        }

        ByteBuffer buffer =
                ByteBuffer.wrap(bytes)
                        .order(
                                ByteOrder.LITTLE_ENDIAN
                        );

        List<Double> embedding =
                new ArrayList<>(
                        bytes.length / Double.BYTES
                );

        while (buffer.hasRemaining()) {

            embedding.add(
                    buffer.getDouble()
            );
        }

        return embedding;
    }

    /**
     * Extracts a simple YAML metadata field.
     */
    private String extractMetadata(
            String content,
            String field) {

        if (content == null
                || content.isBlank()) {

            return "";
        }

        String[] lines =
                content.split("\\R");

        for (String line :
                lines) {

            String trimmed =
                    line.trim();

            String prefix =
                    field + ":";

            if (trimmed.startsWith(prefix)) {

                String value =
                        trimmed.substring(
                                prefix.length()
                        ).trim();

                return removeQuotes(
                        value
                );
            }
        }

        return "";
    }

    /**
     * Extracts YAML tags and stores them
     * as a comma-separated string in SQLite.
     */
    private String extractTags(
            String content) {

        if (content == null
                || content.isBlank()) {

            return "";
        }

        String[] lines =
                content.split("\\R");

        boolean insideTags =
                false;

        List<String> tags =
                new ArrayList<>();

        for (String line :
                lines) {

            String trimmed =
                    line.trim();

            if (trimmed.equals("tags:")) {

                insideTags = true;
                continue;
            }

            if (insideTags) {

                if (trimmed.startsWith("- ")) {

                    String tag =
                            trimmed.substring(
                                    2
                            ).trim();

                    tags.add(
                            removeQuotes(
                                    tag
                            )
                    );

                    continue;
                }

                if (!trimmed.isEmpty()
                        && !trimmed.startsWith("-")) {

                    break;
                }
            }
        }

        return String.join(
                ",",
                tags
        );
    }

    private String removeQuotes(
            String value) {

        if (value == null) {
            return "";
        }

        if (value.length() >= 2
                && value.startsWith("\"")
                && value.endsWith("\"")) {

            return value.substring(
                    1,
                    value.length() - 1
            );
        }

        return value;
    }

    /**
     * Removes the automatically generated
     * Markdown link section.
     *
     * This does NOT modify the actual
     * Markdown document.
     */
    private String removeAutoLinkBlock(
            String content) {

        if (content == null
                || content.isBlank()) {

            return content;
        }

        int startIndex =
                content.indexOf(
                        AUTO_LINK_START
                );

        if (startIndex == -1) {

            return content;
        }

        int endIndex =
                content.indexOf(
                        AUTO_LINK_END,
                        startIndex
                );

        if (endIndex == -1) {

            /*
             * If the start marker exists but
             * the end marker does not, leave
             * the content untouched.
             */
            return content;
        }

        int afterEnd =
                endIndex
                        + AUTO_LINK_END.length();

        return (
                content.substring(
                        0,
                        startIndex
                ).stripTrailing()

                + "\n"

                + content.substring(
                        afterEnd
                ).stripLeading()
        ).trim();
    }

    private String calculateHash(
            String content) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            content.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hex =
                    new StringBuilder();

            for (byte b :
                    hash) {

                hex.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return hex.toString();

        } catch (
                NoSuchAlgorithmException e) {

            throw new RuntimeException(
                    "SHA-256 algorithm not available.",
                    e
            );
        }
    }

    public List<String> searchKeyword(
        String query) {

                List<String> matchingIds =
                        new ArrayList<>();

                if (query == null
                        || query.isBlank()) {

                        return matchingIds;
                }

                String sql = """
                        SELECT id
                        FROM semantic_documents_fts
                        WHERE semantic_documents_fts
                                MATCH ?
                        """;

                try (Connection connection =
                                DriverManager.getConnection(
                                        DATABASE_URL
                                );
                        PreparedStatement statement =
                                connection.prepareStatement(sql)) {

                        statement.setString(
                                1,
                                query
                        );

                        try (ResultSet resultSet =
                                statement.executeQuery()) {

                        while (resultSet.next()) {

                                matchingIds.add(
                                        resultSet.getString("id")
                                );
                        }
                        }

                } catch (SQLException e) {

                        throw new RuntimeException(
                                "Failed to perform FTS5 keyword search.",
                                e
                        );
                }

                return matchingIds;
                }
    public List<DocumentEmbedding>
    getDocumentEmbeddings() {

        return documentEmbeddings;
    }

    /**
     * SQLite representation of one
     * stored semantic embedding.
     *
     * Markdown content is deliberately
     * NOT stored here.
     */
    private record StoredEmbedding(
            String id,
            String title,
            String hash,
            List<Double> embedding
    ) {
    }
}