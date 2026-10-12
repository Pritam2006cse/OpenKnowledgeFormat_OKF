
package com.okf;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8081")
public class ChatController {
    private final OllamaChatService ollamaChatService;

    public ChatController(OllamaChatService ollamaChatService) {
        this.ollamaChatService = ollamaChatService;
    }

    @PostMapping("/chat")
    public ChatResponseDTO chat(
            @RequestBody ChatRequestDTO request) {
                String name = request.getMarkdownName();
                String question = request.getMessage();
                if (name == null || name.isBlank() || question == null || question.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Markdown filename and message are required."
                    );
                }
                if (!name.toLowerCase().endsWith(".md")) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Only Markdown documents can be used."
                    );
                }

                try {
                    Path knowledgeDir = Paths.get("knowledge")
                            .toAbsolutePath()
                            .normalize();

                    Path file = knowledgeDir.resolve(name)
                            .normalize();

                    if (!file.startsWith(knowledgeDir)) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid Markdown filename."
                        );
                    }

                    if (!Files.isRegularFile(file)) {
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Markdown document not found."
                        );
                    }

                    String markdown = Files.readString(file,StandardCharsets.UTF_8);
                    List<ChatMessageDTO> history = request.getHistory();
                    String answer = ollamaChatService.generateAnswer(markdown,question,history);
                    return new ChatResponseDTO(file.getFileName().toString(),answer);

                } catch (IOException e) {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Could not read the Markdown document.",
                            e
                    );
                } catch (ResponseStatusException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Could not generate an answer. "
                                    + "Check that Ollama is running "
                                    + "and the requested model is installed.",
                            e
                    );
                }
    }
}
