
package com.okf;

public class ChatResponseDTO {

    private String markdownName;
    private String answer;
    private String status;
    private String suggestedDocument;

    public ChatResponseDTO() {
    }

    public ChatResponseDTO(String markdownName, String answer,String status,String suggestedDocument) {
        this.markdownName = markdownName;
        this.answer = answer;
        this.status = status;
        this.suggestedDocument = suggestedDocument;
    }

    public String getMarkdownName() {
        return markdownName;
    }

    public String getAnswer() {
        return answer;
    }

    public String getStatus()
    {
        return status;
    }

    public String getSuggestedDocument()
    {
        return suggestedDocument;
    }
}
