package com.okf;

public class TextPreprocessor {
    public static String cleanMarkdown(String markdown) {
        String text = markdown;

        // Remove fenced code blocks
        text = text.replaceAll("(?s)```.*?```"," ");

        // Remove inline code markers
        text = text.replace("`", " ");

        // Remove Markdown headings
        text = text.replaceAll("(?m)^#{1,6}\\s*","");

        // Remove bold / italic markers
        text = text.replaceAll("[*_~]"," ");

        // Remove unordered-list markers
        text = text.replaceAll("(?m)^\\s*[-+]\\s+","");

        // Remove ordered-list markers
        text = text.replaceAll("(?m)^\\s*\\d+\\.\\s+","");

        // Convert multiple spaces/newlines into one space
        text = text.replaceAll("\\s+"," ");

        return text.trim().toLowerCase();
    }
}