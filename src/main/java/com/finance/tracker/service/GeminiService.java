package com.finance.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    /**
     * Core method: sends a prompt to Gemini 1.5 Flash and returns the text response.
     */
    public String generateContent(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                    Map.of("parts", new Object[]{
                        Map.of("text", prompt)
                    })
                },
                "generationConfig", Map.of(
                    "temperature", 0.3,
                    "maxOutputTokens", 1024,
                    "topP", 0.8
                ),
                "safetySettings", new Object[]{
                    Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_NONE"),
                    Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_NONE")
                }
            );

            String url = apiUrl + "?key=" + apiKey;

            String responseBody = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return extractTextFromResponse(responseBody);

        } catch (WebClientResponseException e) {
            log.error("Gemini API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "AI service temporarily unavailable. Please try again.";
        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage());
            return "AI service error: " + e.getMessage();
        }
    }

    /**
     * Extracts the text from Gemini's nested JSON response structure.
     */
    private String extractTextFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                return parts.get(0).path("text").asText();
            }
        }
        log.warn("Unexpected Gemini response format: {}", responseBody);
        return "Could not parse AI response.";
    }

    // ─────────────────────────────────────────────
    // Specialised prompt builders
    // ─────────────────────────────────────────────

    /**
     * Categorizes a raw expense description.
     * Returns one of the known category names.
     */
    public String categorizeExpense(String description, double amount, java.util.List<String> availableCategories) {
        String prompt = String.format("""
            You are a financial expense categorizer. Given an expense description and amount,
            return ONLY the most appropriate category name from this exact list (no other text):
            %s

            Expense: "%s" | Amount: ₹%.2f

            Rules:
            - Return ONLY the category name, nothing else
            - If unsure, return "Other"
            - Swiggy/Zomato = Food, Ola/Uber/Metro = Transport, Netflix/Spotify = Subscriptions
            """,
            String.join(", ", availableCategories),
            description, amount
        );
        return generateContent(prompt).trim();
    }

    /**
     * Extracts merchant name from a description.
     */
    public String extractMerchant(String description) {
        String prompt = String.format("""
            Extract ONLY the merchant/company name from this expense description: "%s"
            Return just the merchant name, nothing else. If no merchant found, return "Unknown".
            """, description);
        return generateContent(prompt).trim();
    }

    /**
     * Generates human-readable spending insights for a given month.
     */
    public String generateSpendingInsights(String spendingJson, int month, int year) {
        String prompt = String.format("""
            You are a personal finance advisor. Analyze this spending data for %d/%d and provide insights.

            Spending data (JSON):
            %s

            Provide a concise analysis (3-4 sentences) covering:
            1. Overall spending pattern
            2. Top spending category observation
            3. One specific actionable tip to save money
            4. A positive encouraging note

            Be friendly, specific, and use Indian Rupee (₹) context.
            """, month, year, spendingJson);
        return generateContent(prompt);
    }

    /**
     * Generates tips list from spending data.
     */
    public String generateSavingTips(String spendingJson) {
        String prompt = String.format("""
            Based on this spending data: %s

            Give exactly 3 practical money-saving tips as a numbered list.
            Each tip should be specific, actionable, and under 20 words.
            Format:
            1. [tip]
            2. [tip]
            3. [tip]
            """, spendingJson);
        return generateContent(prompt);
    }

    /**
     * Chat assistant — answers finance questions with context of user's expense data.
     */
    public String chatWithFinanceData(String userMessage, String expenseSummaryJson) {
        String prompt = String.format("""
            You are a helpful personal finance assistant. Answer the user's question based on their financial data.

            User's expense summary:
            %s

            User's question: "%s"

            Instructions:
            - Be conversational and helpful
            - Use specific numbers from their data when relevant
            - Keep response under 150 words
            - Use ₹ for amounts
            - If the question is unrelated to finance, politely redirect
            """, expenseSummaryJson, userMessage);
        return generateContent(prompt);
    }

    /**
     * Explains why a specific transaction is anomalous.
     */
    public String explainAnomaly(String description, double amount, double avgAmount, String category) {
        String prompt = String.format("""
            Explain in ONE short sentence (max 20 words) why this expense might be unusual:
            - Description: %s
            - Amount: ₹%.2f
            - Category: %s
            - Your average for this category: ₹%.2f

            Be direct and specific. Start with "This is" or "This appears".
            """, description, amount, category, avgAmount);
        return generateContent(prompt).trim();
    }
}
