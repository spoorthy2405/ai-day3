package com.example.docai.service;

import com.example.docai.model.DocumentMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    public DocumentMetadata processDocument(byte[] fileBytes, String contentType) {
        try {
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of(
                        "parts", List.of(
                            Map.of("text", "Extract the following fields from this document in JSON format. Use the exact keys: 'name', 'policy_number', 'date', 'confidence'. 'confidence' should be a value between 0 and 1 representing your certainty."),
                            Map.of("inline_data", Map.of(
                                "mime_type", contentType,
                                "data", base64Content
                            ))
                        )
                    )
                ),
                "generationConfig", Map.of(
                    "response_mime_type", "application/json"
                )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                GEMINI_URL + apiKey,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseGeminiResponse(response.getBody());
            } else {
                log.error("Gemini API call failed with status: {}, body: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Gemini API call failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error processing document with Gemini", e);
            throw new RuntimeException("Document processing failed: " + e.getMessage());
        }
    }

    private DocumentMetadata parseGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("candidates")
                          .get(0)
                          .path("content")
                          .path("parts")
                          .get(0)
                          .path("text")
                          .asText();

        log.debug("Gemini response text: {}", text);
        
        // Clean JSON from potential markdown code blocks
        String jsonResult = cleanJson(text);
        JsonNode metadataNode = objectMapper.readTree(jsonResult);

        return DocumentMetadata.builder()
                .name(metadataNode.path("name").asText())
                .policyNumber(metadataNode.path("policy_number").asText())
                .date(metadataNode.path("date").asText())
                .confidence(metadataNode.path("confidence").asDouble())
                .build();
    }

    private String cleanJson(String text) {
        if (text == null) return "{}";
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}
