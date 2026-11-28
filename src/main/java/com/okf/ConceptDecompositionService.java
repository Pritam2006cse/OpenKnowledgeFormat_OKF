package com.okf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
public class ConceptDecompositionService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String OLLAMA_URL =
            "http://localhost:11434/api/generate";

    private static final String MODEL =
            "llama3.2:3b";

    public ConceptDecompositionService() {

        this.httpClient =
                HttpClient.newHttpClient();

        this.objectMapper =
                new ObjectMapper();
    }

    public List<ConceptNote> decompose(
            String rawMarkdown) throws Exception {

        if (rawMarkdown == null
                || rawMarkdown.isBlank()) {

            throw new IllegalArgumentException(
                    "Raw Markdown is empty."
            );
        }

        String prompt =
                buildPrompt(rawMarkdown);

        String requestBody =
                objectMapper.writeValueAsString(
                        new OllamaRequest(
                                MODEL,
                                prompt,
                                false
                        )
                );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(OLLAMA_URL))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(requestBody)
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {

            throw new IOException(
                    "Ollama request failed. HTTP status: "
                            + response.statusCode()
            );
        }

        OllamaResponse ollamaResponse =
                objectMapper.readValue(
                        response.body(),
                        OllamaResponse.class
                );

        String generatedText =
                ollamaResponse.response();

        return parseConcepts(generatedText);
    }

    private String buildPrompt(
            String rawMarkdown) {

        return """
                You are the concept decomposition engine
                of an Open Knowledge Framework called OKF.

                Your job is to analyze the supplied Markdown
                document and split it into distinct core
                knowledge concepts.

                IMPORTANT RULES:

                1. Identify the actual concepts discussed
                   in the document.

                2. Create one standalone knowledge note
                   for each meaningful concept.

                3. If the entire document represents only
                   one meaningful concept, return exactly
                   one note.

                4. Do not create unnecessary tiny concepts.

                5. Do not invent information that is not
                   present in the source document.

                6. Preserve useful factual information.

                7. Each note must be understandable on its
                   own without requiring the original document.

                8. Generate a clean resource ID using lowercase
                   letters, numbers and underscores only.

                9. The description should briefly explain
                   what the note contains.

                10. Tags should contain useful topic keywords.

                11. Return ONLY valid JSON.
                    Do not use Markdown fences.
                    Do not add explanations before or after
                    the JSON.

                Required JSON format:

                {
                  "concepts": [
                    {
                      "resource": "machine_learning",
                      "title": "Machine Learning",
                      "description": "A brief description of machine learning.",
                      "tags": [
                        "machine learning",
                        "algorithms",
                        "training"
                      ],
                      "content": "# Machine Learning\\n\\n..."
                    }
                  ]
                }

                SOURCE DOCUMENT:

                """ + rawMarkdown;
    }

    private List<ConceptNote> parseConcepts(
            String generatedText)
            throws IOException {

        String cleaned =
                generatedText.trim();

        // Remove accidental Markdown JSON fences.
        if (cleaned.startsWith("```json")) {

            cleaned =
                    cleaned.substring(7).trim();
        }

        if (cleaned.startsWith("```")) {

            cleaned =
                    cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    ).trim();
        }

        ConceptResponse result =
                objectMapper.readValue(
                        cleaned,
                        ConceptResponse.class
                );

        if (result.concepts() == null
                || result.concepts().isEmpty()) {

            throw new IOException(
                    "Ollama returned no concepts."
            );
        }

        return result.concepts();
    }

    public record ConceptNote(
            String resource,
            String title,
            String description,
            List<String> tags,
            String content
    ) {
    }

    private record ConceptResponse(
            List<ConceptNote> concepts
    ) {
    }

    private record OllamaRequest(
            String model,
            String prompt,
            boolean stream
    ) {
    }
        @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaResponse(
            String response
    ) {
    }
}