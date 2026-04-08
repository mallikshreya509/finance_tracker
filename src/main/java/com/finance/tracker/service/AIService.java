package com.finance.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.tracker.dto.AIDTOs.*;
import com.finance.tracker.model.AIChatLog;
import com.finance.tracker.model.Expense;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.AIChatLogRepository;
import com.finance.tracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private static final double ANOMALY_THRESHOLD = 2.0; // Z-score threshold

    private final GeminiService geminiService;
    private final ExpenseRepository expenseRepository;
    private final AIChatLogRepository chatLogRepository;
    private final ObjectMapper objectMapper;

    // ── Chat Assistant ────────────────────────────────────────

    @Transactional
    public ChatResponse chat(String userMessage, User user) {
        // Build financial context summary
        String contextJson = buildExpenseSummaryJson(user);
        String aiResponse = geminiService.chatWithFinanceData(userMessage, contextJson);

        // Persist chat log
        chatLogRepository.save(AIChatLog.builder()
            .user(user)
            .query(userMessage)
            .response(aiResponse)
            .createdAt(LocalDateTime.now())
            .build());

        ChatResponse response = new ChatResponse();
        response.setUserMessage(userMessage);
        response.setAiResponse(aiResponse);
        response.setTimestamp(LocalDateTime.now().toString());
        return response;
    }

    // ── Spending Insights ─────────────────────────────────────

    @Transactional(readOnly = true)
    public SpendingInsightsResponse getMonthlyInsights(User user, int year, int month) {
        List<Expense> currentExpenses = expenseRepository.findByUserAndYearAndMonth(user, year, month);
        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;

        BigDecimal currentTotal = currentExpenses.stream()
            .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevTotal = Optional.ofNullable(
            expenseRepository.sumAmountByUserAndMonth(user, prevYear, prevMonth)
        ).orElse(BigDecimal.ZERO);

        // Category breakdown
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Expense e : currentExpenses) {
            String cat = e.getCategory() != null ? e.getCategory().getName() : "Other";
            byCategory.merge(cat, e.getAmount(), BigDecimal::add);
        }

        String topCategory = byCategory.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("None");

        // Percent change
        BigDecimal changePercent = BigDecimal.ZERO;
        String trend = "stable";
        if (prevTotal.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = currentTotal.subtract(prevTotal)
                .divide(prevTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
            trend = changePercent.compareTo(BigDecimal.ZERO) > 0 ? "up" : "down";
        }

        // Gemini AI analysis
        String spendingJson = buildSpendingJson(byCategory, currentTotal, prevTotal, month, year);
        String aiInsight = geminiService.generateSpendingInsights(spendingJson, month, year);
        String tipsRaw = geminiService.generateSavingTips(spendingJson);
        List<String> tips = parseTips(tipsRaw);

        SpendingInsightsResponse response = new SpendingInsightsResponse();
        response.setMonth(Month.of(month).name() + " " + year);
        response.setTotalSpent(currentTotal);
        response.setPreviousMonthTotal(prevTotal);
        response.setChangePercent(changePercent);
        response.setTrend(trend);
        response.setByCategory(byCategory);
        response.setTopCategory(topCategory);
        response.setAiInsight(aiInsight);
        response.setTips(tips);
        return response;
    }

    // ── Anomaly Detection ─────────────────────────────────────

    @Transactional(readOnly = true)
    public AnomaliesReport detectAnomalies(User user, int year, int month) {
        List<Expense> expenses = expenseRepository.findByUserAndYearAndMonth(user, year, month);
        List<AnomalyResponse> anomalies = new ArrayList<>();

        // Group by category to compute mean and std dev
        Map<Long, List<Expense>> byCategory = expenses.stream()
            .filter(e -> e.getCategory() != null)
            .collect(Collectors.groupingBy(e -> e.getCategory().getId()));

        for (Map.Entry<Long, List<Expense>> entry : byCategory.entrySet()) {
            List<Expense> catExpenses = entry.getValue();
            if (catExpenses.size() < 2) continue;

            double[] amounts = catExpenses.stream()
                .mapToDouble(e -> e.getAmount().doubleValue()).toArray();
            double mean = Arrays.stream(amounts).average().orElse(0);
            double std = computeStdDev(amounts, mean);
            if (std == 0) continue;

            for (Expense e : catExpenses) {
                double zScore = Math.abs((e.getAmount().doubleValue() - mean) / std);
                if (zScore >= ANOMALY_THRESHOLD) {
                    String explanation = geminiService.explainAnomaly(
                        e.getDescription(),
                        e.getAmount().doubleValue(),
                        mean,
                        e.getCategory().getName()
                    );
                    AnomalyResponse anomaly = new AnomalyResponse();
                    anomaly.setExpenseId(e.getId());
                    anomaly.setDescription(e.getDescription());
                    anomaly.setAmount(e.getAmount());
                    anomaly.setAvgForCategory(BigDecimal.valueOf(mean).setScale(2, RoundingMode.HALF_UP));
                    anomaly.setZScore(Math.round(zScore * 100.0) / 100.0);
                    anomaly.setSeverity(zScore >= 3.0 ? "HIGH" : "MEDIUM");
                    anomaly.setAiExplanation(explanation);
                    anomalies.add(anomaly);
                }
            }
        }

        anomalies.sort(Comparator.comparingDouble(AnomalyResponse::getZScore).reversed());

        String summary = anomalies.isEmpty()
            ? "No unusual spending patterns detected this month. Keep it up! 🎉"
            : String.format("Found %d unusual transaction(s) this month. Review them carefully.", anomalies.size());

        AnomaliesReport report = new AnomaliesReport();
        report.setTotalAnomalies(anomalies.size());
        report.setAnomalies(anomalies);
        report.setSummary(summary);
        return report;
    }

    // ── Categorize single text ────────────────────────────────

    public CategorizationResult categorize(String text, double amount) {
        List<String> categories = List.of(
            "Food", "Transport", "Shopping", "Entertainment", "Health",
            "Utilities", "Education", "Travel", "Groceries", "Rent", "Subscriptions", "Other"
        );
        String category = geminiService.categorizeExpense(text, amount, categories);
        String merchant = geminiService.extractMerchant(text);

        CategorizationResult result = new CategorizationResult();
        result.setCategory(category.trim());
        result.setMerchant(merchant.trim());
        result.setConfidence("HIGH");
        result.setReasoning("Categorized by Gemini 1.5 Flash AI");
        return result;
    }

    // ── Chat history ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChatResponse> getChatHistory(User user, int page, int size) {
        return chatLogRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(page, size))
            .getContent().stream().map(log -> {
                ChatResponse r = new ChatResponse();
                r.setUserMessage(log.getQuery());
                r.setAiResponse(log.getResponse());
                r.setTimestamp(log.getCreatedAt().toString());
                return r;
            }).toList();
    }

    // ── Private helpers ───────────────────────────────────────

    private String buildExpenseSummaryJson(User user) {
        try {
            List<Expense> recent = expenseRepository.findByUserOrderByExpenseDateDesc(user)
                .stream().limit(50).toList();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("totalTransactions", recent.size());
            BigDecimal total = recent.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.put("totalSpent", total);

            Map<String, BigDecimal> byCat = new LinkedHashMap<>();
            for (Expense e : recent) {
                String cat = e.getCategory() != null ? e.getCategory().getName() : "Other";
                byCat.merge(cat, e.getAmount(), BigDecimal::add);
            }
            summary.put("spendingByCategory", byCat);

            List<Map<String, Object>> recentList = recent.stream().limit(10).map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("description", e.getDescription());
                m.put("amount", e.getAmount());
                m.put("category", e.getCategory() != null ? e.getCategory().getName() : "Other");
                m.put("date", e.getExpenseDate().toString());
                return m;
            }).toList();
            summary.put("recentExpenses", recentList);
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildSpendingJson(Map<String, BigDecimal> byCategory,
                                     BigDecimal current, BigDecimal prev,
                                     int month, int year) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("month", Month.of(month).name());
            data.put("year", year);
            data.put("totalSpent", current);
            data.put("previousMonthTotal", prev);
            data.put("spendingByCategory", byCategory);
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private double computeStdDev(double[] values, double mean) {
        double variance = Arrays.stream(values)
            .map(v -> Math.pow(v - mean, 2))
            .average().orElse(0);
        return Math.sqrt(variance);
    }

    private List<String> parseTips(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("\n"))
            .map(line -> line.replaceAll("^\\d+\\.\\s*", "").trim())
            .filter(line -> !line.isBlank())
            .limit(3)
            .toList();
    }
}
