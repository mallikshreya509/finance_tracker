package com.finance.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExpenseDTOs {

    @Data
    public static class CreateExpenseRequest {
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        @NotBlank(message = "Description is required")
        private String description;

        private Long categoryId;       // optional — AI fills it if absent
        private LocalDate expenseDate;
        private String notes;
        private boolean useAiCategory = true;
    }

    @Data
    public static class UpdateExpenseRequest {
        @Positive
        private BigDecimal amount;
        private String description;
        private Long categoryId;
        private LocalDate expenseDate;
        private String notes;
    }

    @Data
    public static class ExpenseResponse {
        private Long id;
        private BigDecimal amount;
        private String description;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private LocalDate expenseDate;
        private boolean aiCategorized;
        private String merchant;
        private String notes;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ExpensePageResponse {
        private java.util.List<ExpenseResponse> expenses;
        private long totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;
    }
}
