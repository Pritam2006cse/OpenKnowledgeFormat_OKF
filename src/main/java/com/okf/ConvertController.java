package com.okf;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8081")
public class ConvertController {
         private final SearchService searchService;

        public ConvertController(SearchService searchService) {
                this.searchService = searchService;
        }
    @PostMapping("/convert")
    public ResponseEntity<?> convertPdf(
            @RequestParam("file") MultipartFile file) {

        try {
                System.out.println("APPLICATION WORKING DIRECTORY: "+ System.getProperty("user.dir"));
            System.out.println("Received file: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("File is empty");
            }

            String originalName = file.getOriginalFilename();

            if (originalName == null) {
                return ResponseEntity.badRequest()
                        .body("Invalid file name");
            }

            String baseName = originalName
                    .replaceFirst("[.][^.]+$", "")
                    .replaceAll("[^a-zA-Z0-9_-]", "_");

            // Get the backend project's working directory
                Path projectDirectory = Path.of("")
                        .toAbsolutePath()
                        .normalize();

                // Define storage directories
                Path pdfDirectory = projectDirectory.resolve("pdfs");
                Path knowledgeDirectory = projectDirectory.resolve("knowledge");

                // Create directories if they do not exist
                Files.createDirectories(pdfDirectory);
                Files.createDirectories(knowledgeDirectory);

                // Define absolute file paths
                Path pdfPath = pdfDirectory.resolve(baseName + ".pdf");
                Path markdownPath = knowledgeDirectory.resolve(baseName + ".md");

                // Python conversion script
                Path pythonScript = projectDirectory.resolve("pdf_to_markdown.py");

                System.out.println("Project Directory: " + projectDirectory);
                System.out.println("PDF Path: " + pdfPath);
                System.out.println("Markdown Path: " + markdownPath);
                System.out.println("Python Script: " + pythonScript);

            Files.write(
                    pdfPath,
                    file.getBytes()
            );

            System.out.println(
                    "PDF saved to: " + pdfPath.toAbsolutePath()
            );

            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            "python",
                            "pdf_to_markdown.py",
                            pdfPath.toString(),
                            markdownPath.toString()
                    );

            processBuilder.redirectErrorStream(true);

            Process process =
                    processBuilder.start();

            String output =
                    new String(
                            process.getInputStream().readAllBytes(),
                            StandardCharsets.UTF_8
                    );

            int exitCode =
                    process.waitFor();

            System.out.println(
                    "Python output:\n" + output
            );

            System.out.println(
                    "Python exit code: " + exitCode
            );

            if (exitCode != 0) {

                return ResponseEntity
                        .internalServerError()
                        .body(
                                "PDF conversion failed:\n"
                                + output
                        );
            }

            String markdown =
        Files.readString(
                markdownPath,
                StandardCharsets.UTF_8
        );

// Reload the OKF search index so the new
// Markdown document becomes searchable.
searchService.reloadIndex();

return ResponseEntity.ok(
        new ConvertResponse(
                markdownPath.getFileName().toString(),
                markdown
        )
);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Conversion error: "
                            + e.getMessage()
                    );
        }
    }

    public static class ConvertResponse {

        private final String markdownName;
        private final String markdown;

        public ConvertResponse(
                String markdownName,
                String markdown) {

            this.markdownName = markdownName;
            this.markdown = markdown;
        }

        public String getMarkdownName() {
            return markdownName;
        }

        public String getMarkdown() {
            return markdown;
        }
    }
}