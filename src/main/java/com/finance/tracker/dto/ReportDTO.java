package com.finance.tracker.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ReportDTO {
    private int year;
    private int month;
    private String monthName;
    private BigDecimal totalSpent;
    private BigDecimal avgDailySpend;
    private int totalTransactions;
    private Map<String, BigDecimal> spendingByCategory;
    private List<ExpenseDTOs.ExpenseResponse> topExpenses;
    private BigDecimal previousMonthTotal;
    private double changeFromLastMonth;
}
