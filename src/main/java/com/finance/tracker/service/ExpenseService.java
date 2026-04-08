package com.finance.tracker.service;

import com.finance.tracker.dto.ExpenseDTOs.*;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.model.Category;
import com.finance.tracker.model.Expense;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final GeminiService geminiService;
    private final CategoryService categoryService;

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest req, User user) {
        Category category;
        boolean aiCategorized = false;

        if (req.getCategoryId() != null) {
            // Manual category provided
            category = categoryService.getById(req.getCategoryId());
        } else if (req.isUseAiCategory()) {
            // Ask Gemini to categorize
            category = aiCategorize(req.getDescription(), req.getAmount());
            aiCategorized = true;
        } else {
            // Fallback to "Other"
            category = categoryRepository.findByNameIgnoreCase("Other")
                .orElseThrow(() -> new BadRequestException("Default category not found"));
        }

        // Extract merchant via AI (lightweight call)
        String merchant = null;
        try {
            merchant = geminiService.extractMerchant(req.getDescription());
            if ("Unknown".equalsIgnoreCase(merchant)) merchant = null;
        } catch (Exception e) {
            log.warn("Merchant extraction failed, skipping: {}", e.getMessage());
        }

        Expense expense = Expense.builder()
            .user(user)
            .amount(req.getAmount())
            .description(req.getDescription())
            .category(category)
            .expenseDate(req.getExpenseDate() != null ? req.getExpenseDate() : LocalDate.now())
            .aiCategorized(aiCategorized)
            .merchant(merchant)
            .notes(req.getNotes())
            .build();

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, UpdateExpenseRequest req, User user) {
        Expense expense = getExpenseForUser(id, user);
        if (req.getAmount() != null) expense.setAmount(req.getAmount());
        if (req.getDescription() != null) expense.setDescription(req.getDescription());
        if (req.getExpenseDate() != null) expense.setExpenseDate(req.getExpenseDate());
        if (req.getNotes() != null) expense.setNotes(req.getNotes());
        if (req.getCategoryId() != null) {
            expense.setCategory(categoryService.getById(req.getCategoryId()));
            expense.setAiCategorized(false);
        }
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(Long id, User user) {
        Expense expense = getExpenseForUser(id, user);
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public ExpensePageResponse getExpenses(User user, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("expenseDate").descending());
        Page<Expense> expensePage = expenseRepository.findByUserOrderByExpenseDateDesc(user, pageable);

        ExpensePageResponse response = new ExpensePageResponse();
        response.setExpenses(expensePage.getContent().stream().map(this::toResponse).toList());
        response.setTotalElements(expensePage.getTotalElements());
        response.setTotalPages(expensePage.getTotalPages());
        response.setCurrentPage(page);
        response.setPageSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id, User user) {
        return toResponse(getExpenseForUser(id, user));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByMonth(User user, int year, int month) {
        return expenseRepository.findByUserAndYearAndMonth(user, year, month)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> searchExpenses(User user, String keyword) {
        return expenseRepository.searchByDescription(user, keyword)
            .stream().map(this::toResponse).toList();
    }

    // ── Helpers ──────────────────────────────────────────────

    private Category aiCategorize(String description, BigDecimal amount) {
        List<String> categoryNames = categoryRepository.findAll()
            .stream().map(Category::getName).toList();
        String aiCategory = geminiService.categorizeExpense(description, amount.doubleValue(), categoryNames);

        return categoryRepository.findByNameIgnoreCase(aiCategory.trim())
            .orElseGet(() -> categoryRepository.findByNameIgnoreCase("Other").orElseThrow());
    }

    private Expense getExpenseForUser(Long id, User user) {
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Access denied to this expense");
        }
        return expense;
    }

    public ExpenseResponse toResponse(Expense e) {
        ExpenseResponse r = new ExpenseResponse();
        r.setId(e.getId());
        r.setAmount(e.getAmount());
        r.setDescription(e.getDescription());
        r.setExpenseDate(e.getExpenseDate());
        r.setAiCategorized(e.isAiCategorized());
        r.setMerchant(e.getMerchant());
        r.setNotes(e.getNotes());
        r.setCreatedAt(e.getCreatedAt());
        if (e.getCategory() != null) {
            r.setCategoryName(e.getCategory().getName());
            r.setCategoryIcon(e.getCategory().getIcon());
            r.setCategoryColor(e.getCategory().getColorHex());
        }
        return r;
    }
}
