
package com.okf;

import java.util.ArrayList;
import java.util.List;

public class PassageRetriever {

    public String findBestPassage(String content, String query) {

        if (content == null || content.isBlank()
                || query == null || query.isBlank()) {
            return "No relevant passage found.";
        }

        List<String> queryTerms =
                TextPreprocessor.process(query);

        if (queryTerms.isEmpty()) {
            return "No relevant passage found.";
        }

        List<String> passages = splitIntoPassages(content);

        String bestPassage = null;
        double bestScore = 0.0;

        for (String passage : passages) {

            String cleanPassage =
                    TextPreprocessor.cleanMarkdown(passage);

            List<String> passageTerms =
                    TextPreprocessor.process(cleanPassage);

            if (passageTerms.isEmpty()) {
                continue;
            }

            int matchedTerms = 0;

            for (String term : queryTerms) {
                if (passageTerms.contains(term)) {
                    matchedTerms++;
                }
            }

            double score =
                    (double) matchedTerms / queryTerms.size();

            if (score > bestScore) {
                bestScore = score;
                bestPassage = cleanPassage;
            }
        }

        if (bestPassage == null) {
            return "No relevant passage found.";
        }

        return bestPassage;
    }

    private List<String> splitIntoPassages(String content) {

        List<String> passages = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        String[] lines = content.split("\\R");

        for (String line : lines) {

            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            if (isHeading(trimmed)) {

                addPassage(passages, current);

                current.append(trimmed).append(" ");

            } else {

                current.append(trimmed).append(" ");
            }
        }

        addPassage(passages, current);

        return passages;
    }

    private boolean isHeading(String line) {

        String candidate = line.trim();

        // Handle escaped Markdown headings such as \##1.
        candidate = candidate.replaceFirst("^\\\\+", "");

        // Handle titles such as **# OKF Sample Knowledge**.
        candidate = candidate
                .replaceAll("^\\*{1,2}", "")
                .replaceAll("\\*{1,2}$", "")
                .trim();

        // Standard headings and headings without a space,
        // such as ##1.
        if (candidate.matches("^#{1,6}\\s*\\S.*")) {
            return true;
        }

        // Recognize short, standalone section names such as
        // TCP, IP, DNS, Introduction and Test Keywords.
        return candidate.length() <= 55
                && candidate.matches(
                        "^[A-Z0-9][A-Za-z0-9 ()/&-]*$"
                )
                && !candidate.matches(".*[.!?,;:]$");
    }

    private void addPassage(
            List<String> passages,
            StringBuilder current) {

        if (current.length() > 0) {
            String passage = current.toString().trim();

            if (!passage.isEmpty()) {
                passages.add(passage);
            }

            current.setLength(0);
        }
    }
}
