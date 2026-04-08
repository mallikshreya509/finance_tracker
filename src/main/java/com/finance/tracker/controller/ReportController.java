package com.finance.tracker.controller;

import com.finance.tracker.dto.ApiResponse;
import com.finance.tracker.dto.ReportDTO;
import com.finance.tracker.model.User;
import com.finance.tracker.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Spending reports and analytics")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    @Operation(summary = "Get monthly spending report (defaults to current month)")
    public ResponseEntity<ApiResponse<ReportDTO>> monthly(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal User user) {
        int y = year != null ? year : LocalDate.now().getYear();
        int m = month != null ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(ApiResponse.success(reportService.getMonthlyReport(user, y, m)));
    }
}
