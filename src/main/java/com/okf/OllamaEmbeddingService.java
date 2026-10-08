package com.okf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OllamaEmbeddingService
{
    private static final String ollama_url = "http://localhost:11434/api/embed";
    private static final String ollama_model = "nomic-embed-text-v2-moe";
    private static final String document_prefix = "search_document: ";
    private static final String query_prefix = "search_query: ";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public OllamaEmbeddingService()
    {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
    }
    public List<Double> getDocumentEmbedding(String text)
    {
        if(text == null || text.isBlank())
        {
            return Collections.emptyList();
        }
        return getEmbedding(document_prefix+text);
    }
    public List<Double> getQueryEmbedding(String query)
    {
        if(query == null || query.isBlank())
        {
            return Collections.emptyList();
        }
        return getEmbedding(query_prefix+query);
    }
    private List<Double> getEmbedding(String input)
    {
        try
        {
            String jsonBody = objectMapper.writeValueAsString(new EmbedRequest(ollama_model,List.of(input)));
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ollama_url)).timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) 
            {
                throw new RuntimeException("Ollama returned unexpected HTTP status: " + response.statusCode() + " - " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode embeddingNode = root.get("embeddings");
            JsonNode vectorNode = embeddingNode.get(0);
            List<Double> embedding = new ArrayList<>(vectorNode.size());
            for(JsonNode value: vectorNode)
            {
                embedding.add(value.asDouble());
            }
            return embedding;
        }
        catch(IOException | InterruptedException e)
        {
            if(e instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to generate vector embedings");
        }
    }
    private record EmbedRequest(String model, List<String> input) 
    {
    }
}