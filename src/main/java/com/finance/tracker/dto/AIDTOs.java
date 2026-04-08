package com.finance.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AIDTOs {

    @Data
    public static class ChatRequest {
        @NotBlank(message = "Message cannot be empty")
        private String message;
    }

    @Data
    public static class ChatResponse {
        private String userMessage;
        private String aiResponse;
        private String timestamp;
    }

    @Data
    public static class CategorizationResult {
        private String category;
        private String merchant;
        private String confidence;
        private String reasoning;
    }

    @Data
    public static class SpendingInsightsResponse {
        private String month;
        private BigDecimal totalSpent;
        private BigDecimal previousMonthTotal;
        private BigDecimal changePercent;
        private String trend;
        private Map<String, BigDecimal> byCategory;
        private String topCategory;
        private String aiInsight;
        private List<String> tips;
    }

    @Data
    public static class AnomalyResponse {
        private Long expenseId;
        private String description;
        private BigDecimal amount;
        private BigDecimal avgForCategory;
        private double zScore;
        private String severity;
        private String aiExplanation;
    }

    @Data
    public static class AnomaliesReport {
        private int totalAnomalies;
        private List<AnomalyResponse> anomalies;
        private String summary;
    }
}
