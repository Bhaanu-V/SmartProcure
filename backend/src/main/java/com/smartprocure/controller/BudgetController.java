package com.smartprocure.controller;

import com.smartprocure.dto.budget.BudgetAllocationRequest;
import com.smartprocure.dto.budget.BudgetResponse;
import com.smartprocure.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST controller for department budget allocation, real-time balances, and availability checks.
 */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping("/allocate")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<BudgetResponse> allocateBudget(
            @Valid @RequestBody BudgetAllocationRequest request) {
        BudgetResponse response = budgetService.allocateBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/department/{departmentId}/year/{fiscalYear}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BudgetResponse> getBudget(
            @PathVariable Long departmentId,
            @PathVariable int fiscalYear) {
        BudgetResponse response = budgetService.getBudget(departmentId, fiscalYear);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BudgetResponse>> getDepartmentBudgets(@PathVariable Long departmentId) {
        List<BudgetResponse> response = budgetService.getDepartmentBudgets(departmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department/{departmentId}/year/{fiscalYear}/availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> checkAvailability(
            @PathVariable Long departmentId,
            @PathVariable int fiscalYear,
            @RequestParam BigDecimal amount) {
        boolean available = budgetService.checkBudgetAvailability(departmentId, fiscalYear, amount);
        return ResponseEntity.ok(Collections.singletonMap("available", available));
    }
}
