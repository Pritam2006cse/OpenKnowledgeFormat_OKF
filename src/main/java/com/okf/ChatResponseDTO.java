
package com.okf;

public class ChatResponseDTO {

    private String markdownName;
    private String answer;

    public ChatResponseDTO() {
    }

    public ChatResponseDTO(String markdownName, String answer) {
        this.markdownName = markdownName;
        this.answer = answer;
    }

    public String getMarkdownName() {
        return markdownName;
    }

    public String getAnswer() {
        return answer;
    }
}
