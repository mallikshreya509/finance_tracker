package com.finance.tracker.service;

import com.finance.tracker.dto.ExpenseDTOs.ExpenseResponse;
import com.finance.tracker.dto.ReportDTO;
import com.finance.tracker.model.Expense;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseService expenseService;

    @Transactional(readOnly = true)
    public ReportDTO getMonthlyReport(User user, int year, int month) {
        List<Expense> expenses = expenseRepository.findByUserAndYearAndMonth(user, year, month);

        BigDecimal total = expenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Category breakdown
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Expense e : expenses) {
            String cat = e.getCategory() != null ? e.getCategory().getName() : "Other";
            byCategory.merge(cat, e.getAmount(), BigDecimal::add);
        }

        // Daily average
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        BigDecimal avgDaily = expenses.isEmpty()
            ? BigDecimal.ZERO
            : total.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        // Previous month total
        int prevYear = month == 1 ? year - 1 : year;
        int prevMonth = month == 1 ? 12 : month - 1;
        BigDecimal prevTotal = expenseRepository.sumAmountByUserAndMonth(user, prevYear, prevMonth);
        if (prevTotal == null) prevTotal = BigDecimal.ZERO;

        double changePercent = 0.0;
        if (prevTotal.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = total.subtract(prevTotal)
                .divide(prevTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
        }

        // Top 5 expenses
        List<ExpenseResponse> topExpenses = expenses.stream()
            .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
            .limit(5)
            .map(expenseService::toResponse)
            .toList();

        ReportDTO report = new ReportDTO();
        report.setYear(year);
        report.setMonth(month);
        report.setMonthName(Month.of(month).name());
        report.setTotalSpent(total);
        report.setAvgDailySpend(avgDaily);
        report.setTotalTransactions(expenses.size());
        report.setSpendingByCategory(byCategory);
        report.setTopExpenses(topExpenses);
        report.setPreviousMonthTotal(prevTotal);
        report.setChangeFromLastMonth(changePercent);
        return report;
    }
}
