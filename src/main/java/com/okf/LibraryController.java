package com.okf;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8081")
public class LibraryController {

    @GetMapping("/library")
    public List<LibraryDocumentDTO> getLibrary()
            throws IOException {

        List<LibraryDocumentDTO> documents =
                new ArrayList<>();

        Path directory =
                Paths.get("knowledge");

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(
                             directory,
                             "*.md")) {

            for (Path path : stream) {

                String content =
                        Files.readString(path);

                String title =
                        extractTitle(content);

                String description =
                        extractDescription(content);

                String fileName =
                        path.getFileName().toString();

                documents.add(
                        new LibraryDocumentDTO(
                                title,
                                description,
                                fileName
                        )
                );
            }
        }

        return documents;
    }

    private String extractTitle(
            String content) {

        for (String line :
                content.split("\\R")) {

            if (line.startsWith("# ")) {
                return line.substring(2).trim();
            }
        }

        return "Untitled";
    }

    private String extractDescription(
            String content) {

        boolean titleFound = false;

        for (String line :
                content.split("\\R")) {

            String trimmed =
                    line.trim();

            if (trimmed.startsWith("# ")) {
                titleFound = true;
                continue;
            }

            if (titleFound
                    && !trimmed.isEmpty()
                    && !trimmed.startsWith("#")
                    && !trimmed.startsWith("---")) {

                return trimmed.length() > 200
                        ? trimmed.substring(0, 200) + "..."
                        : trimmed;
            }
        }

        return "No description available.";
    }
}