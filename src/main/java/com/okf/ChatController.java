package com.okf;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8081")
public class ChatController {
    private final OllamaChatService ollamaChatService;
    private final MarkdownContextService markdownContextService;
    private final SearchService searchService;
    private static final double CROSS_DOCUMENT_THRESHOLD = 0.60;

    public ChatController(OllamaChatService ollamaChatService,MarkdownContextService markdownContextService,SearchService searchService) {
        this.ollamaChatService = ollamaChatService;
        this.markdownContextService = markdownContextService;
        this.searchService = searchService;
    }
    @PostMapping("/chat")
    public ChatResponseDTO chat(@RequestBody ChatRequestDTO request) {
        String name = request.getMarkdownName();
        String question = request.getMessage();
        String approvedDocument = request.getApprovedDocument();

        if (name == null || name.isBlank() || question == null || question.isBlank()) {
            throw new ResponseStatusException( HttpStatus.BAD_REQUEST,"Markdown filename and message are required.");
        }
        validateFilename(name);
        try {
            /*
             * =====================================================
             * CASE 1:
             * User has already approved another document.
             * =====================================================
             */
            if (approvedDocument != null && !approvedDocument.isBlank()) {
                validateFilename(approvedDocument);
                String context = markdownContextService.buildContext(approvedDocument,question);

                if (context.isBlank()) {

                    return new ChatResponseDTO(
                            name,
                            "The approved document does not contain enough information to answer this question.",
                            "NO_EVIDENCE",
                            null
                    );
                }

                List<ChatMessageDTO> history =
                        request.getHistory();

                String answer =
                        ollamaChatService.generateAnswer(
                                context,question,
                                history
                        );

                return new ChatResponseDTO(
                        name,
                        answer,
                        "ANSWER",
                        null
                );
            }

            /*
             * =====================================================
             * CASE 2:
             * Try the document currently selected by the user.
             * =====================================================
             */
            String context = markdownContextService.buildContext(name,question);

            if (!context.isBlank()) {

                List<ChatMessageDTO> history = request.getHistory();

                String answer = ollamaChatService.generateAnswer(context,question,history);

                return new ChatResponseDTO(
                        name,
                        answer,
                        "ANSWER",
                        null
                );
            }

            /*
             * =====================================================
             * CASE 3:
             * Current document has no evidence.
             *
             * Now search the entire OKF Library.
             * =====================================================
             */
            String otherDocument = searchService.findBestOtherDocumentFromMetadata(question,name);
            if (otherDocument != null) {
                System.out.println("Metadata cross-document match found: " + otherDocument);
                return new ChatResponseDTO(
                        name,
                        "This question appears to be related to another topic in your OKF Library. I found relevant information in "
                                + otherDocument
                                + ". Would you like me to use that document to answer your question?",
                        "CROSS_DOCUMENT_PERMISSION",
                        otherDocument
                );
            }
            /*
             * =====================================================
             * CASE 4:
             * No sufficient evidence anywhere.
             * =====================================================
             */
            return new ChatResponseDTO(
                    name,
                    "This information is not available in the selected document or the OKF Library.",
                    "NO_EVIDENCE",
                    null
            );
        } catch (ResponseStatusException e) {
            /*
             * Preserve errors such as:
             * 400 Invalid filename
             * 404 Markdown not found
             */
            throw e;
        } catch (Exception e) {
            /*
             * Do not incorrectly report every backend/Ollama
             * failure as a 404.
             */
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Could not generate an answer. Check that Ollama is running and the knowledge index is available.",e);
        }
    }

    private void validateFilename(String name) {
        if (!name.toLowerCase().endsWith(".md")|| name.contains("/")|| name.contains("\\")|| name.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Markdown filename.");
        }
    }
}