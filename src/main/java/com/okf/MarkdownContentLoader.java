package com.okf;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class MarkdownContentLoader {

    private final Path knowledgeDirectory =
            Path.of("knowledge");

    public String load(String documentId)
            throws IOException {

        Path file =
                knowledgeDirectory.resolve(documentId);

        if (!Files.exists(file)) {
            throw new IOException(
                    "Markdown file not found: "
                            + documentId
            );
        }

        return Files.readString(
                file,
                StandardCharsets.UTF_8
        );
    }
}