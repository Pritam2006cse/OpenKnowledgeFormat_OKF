package com.okf;

public class LibraryDocumentDTO {

    private String title;
    private String description;
    private String fileName;

    public LibraryDocumentDTO(
            String title,
            String description,
            String fileName) {

        this.title = title;
        this.description = description;
        this.fileName = fileName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getFileName() {
        return fileName;
    }
}