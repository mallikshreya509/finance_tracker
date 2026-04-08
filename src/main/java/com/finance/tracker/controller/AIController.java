package com.finance.tracker.controller;

import com.finance.tracker.dto.AIDTOs.*;
import com.finance.tracker.dto.ApiResponse;
import com.finance.tracker.model.User;
import com.finance.tracker.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Features", description = "Gemini-powered AI features: chat, insights, anomalies, categorization")
public class AIController {

    private final AIService aiService;

    @PostMapping("/chat")
    @Operation(summary = "Chat with your finance assistant (powered by Gemini 1.5 Flash)")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {
        ChatResponse response = aiService.chat(request.getMessage(), user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/chat/history")
    @Operation(summary = "Get chat history")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> chatHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(aiService.getChatHistory(user, page, size)));
    }

    @GetMapping("/insights")
    @Operation(summary = "Get AI-powered monthly spending insights")
    public ResponseEntity<ApiResponse<SpendingInsightsResponse>> insights(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal User user) {
        int y = year != null ? year : LocalDate.now().getYear();
        int m = month != null ? month : LocalDate.now().getMonthValue();
        SpendingInsightsResponse response = aiService.getMonthlyInsights(user, y, m);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/anomalies")
    @Operation(summary = "Detect unusual spending patterns using AI + statistical analysis")
    public ResponseEntity<ApiResponse<AnomaliesReport>> anomalies(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal User user) {
        int y = year != null ? year : LocalDate.now().getYear();
        int m = month != null ? month : LocalDate.now().getMonthValue();
        AnomaliesReport report = aiService.detectAnomalies(user, y, m);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/categorize")
    @Operation(summary = "Categorize a raw expense description using AI")
    public ResponseEntity<ApiResponse<CategorizationResult>> categorize(
            @RequestParam String text,
            @RequestParam(defaultValue = "0") double amount) {
        CategorizationResult result = aiService.categorize(text, amount);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
