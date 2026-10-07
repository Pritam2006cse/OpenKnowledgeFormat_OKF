package com.okf;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public static List<String> process(String text)
    {
        List<String> terms = new ArrayList<>();
        String cleaned = cleanMarkdown(text);
        try{
            Analyzer analyzer = new EnglishAnalyzer();
            TokenStream tokenStream = analyzer.tokenStream("content", cleaned);
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                   terms.add(termAttribute.toString());
                }
            tokenStream.end();
        }
        catch(IOException e){
            throw new RuntimeException("Error processing text",e);
        }
        return terms;
    }
}