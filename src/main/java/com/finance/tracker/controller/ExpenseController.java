package com.finance.tracker.controller;

import com.finance.tracker.dto.ApiResponse;
import com.finance.tracker.dto.ExpenseDTOs.*;
import com.finance.tracker.model.User;
import com.finance.tracker.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Manage your expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @Operation(summary = "Add a new expense (AI auto-categorizes if no categoryId provided)")
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal User user) {
        ExpenseResponse response = expenseService.createExpense(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Expense added", response));
    }

    @GetMapping
    @Operation(summary = "Get paginated expenses")
    public ResponseEntity<ApiResponse<ExpensePageResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getExpenses(user, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific expense by ID")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getExpenseById(id, user)));
    }

    @GetMapping("/month")
    @Operation(summary = "Get expenses for a specific month")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getByMonth(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getExpensesByMonth(user, year, month)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search expenses by description keyword")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> search(
            @RequestParam String keyword,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.searchExpenses(user, keyword)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(
            @PathVariable Long id,
            @RequestBody UpdateExpenseRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Expense updated",
            expenseService.updateExpense(id, request, user)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an expense")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        expenseService.deleteExpense(id, user);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted", null));
    }
}
