
package com.okf;

import java.util.ArrayList;
import java.util.List;

public class ChatRequestDTO {

    private String markdownName;
    private String message;
    private List<ChatMessageDTO> history = new ArrayList<>();

    public ChatRequestDTO() {
    }

    public String getMarkdownName() {
        return markdownName;
    }

    public void setMarkdownName(String markdownName) {
        this.markdownName = markdownName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatMessageDTO> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessageDTO> history) {
        this.history = history;
    }
}
