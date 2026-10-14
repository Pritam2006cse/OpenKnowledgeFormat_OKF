
package com.okf;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8081")
public class ChatController {

    private final OllamaChatService ollamaChatService;
    private final MarkdownContextService markdownContextService;

    public ChatController(
            OllamaChatService ollamaChatService,
            MarkdownContextService markdownContextService) {

        this.ollamaChatService = ollamaChatService;
        this.markdownContextService = markdownContextService;
    }

    @PostMapping("/chat")
    public ChatResponseDTO chat(
            @RequestBody ChatRequestDTO request) {

        String name = request.getMarkdownName();
        String question = request.getMessage();

        if (name == null || name.isBlank()
                || question == null || question.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Markdown filename and message are required."
            );
        }

        if (!name.toLowerCase().endsWith(".md")
                || name.contains("/") || name.contains("\\")
                || name.contains("..")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Markdown filename."
            );
        }

        try {
            String context = markdownContextService.buildContext(
                    name, question);

            List<ChatMessageDTO> history = request.getHistory();

            String answer = ollamaChatService.generateAnswer(
                    context, question, history);

            return new ChatResponseDTO(name, answer);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Could not read the selected Markdown document.",
                    e
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not generate an answer. Check that Ollama "
                            + "is running and the requested model is installed.",
                    e
            );
        }
    }
}
