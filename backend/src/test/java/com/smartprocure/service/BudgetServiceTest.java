package com.smartprocure.service;

import com.smartprocure.dto.budget.BudgetAllocationRequest;
import com.smartprocure.dto.budget.BudgetResponse;
import com.smartprocure.entity.Department;
import com.smartprocure.entity.DepartmentBudget;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.DepartmentBudgetRepository;
import com.smartprocure.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private DepartmentBudgetRepository budgetRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private BudgetService budgetService;

    private Department department;
    private DepartmentBudget budget;

    @BeforeEach
    void setUp() {
        department = new Department("ENG", "Engineering", "Software & Hardware");
        department.setId(10L);

        budget = new DepartmentBudget(department, 2026, new BigDecimal("50000.00"));
        budget.setId(100L);
    }

    @Test
    @DisplayName("Allocate budget creates new allocation when none exists")
    void testAllocateBudget_New() {
        BudgetAllocationRequest request = new BudgetAllocationRequest(10L, 2026, new BigDecimal("75000.00"));

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(budgetRepository.findByDepartmentIdAndFiscalYear(10L, 2026)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(DepartmentBudget.class))).thenAnswer(inv -> inv.getArgument(0));

        BudgetResponse response = budgetService.allocateBudget(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("75000.00"), response.getAllocatedAmount());
        assertEquals(2026, response.getFiscalYear());
        assertEquals("Engineering", response.getDepartmentName());
    }

    @Test
    @DisplayName("Check budget availability returns correct boolean")
    void testCheckBudgetAvailability() {
        when(budgetRepository.findByDepartmentIdAndFiscalYear(10L, 2026)).thenReturn(Optional.of(budget));

        assertTrue(budgetService.checkBudgetAvailability(10L, 2026, new BigDecimal("49999.99")));
        assertTrue(budgetService.checkBudgetAvailability(10L, 2026, new BigDecimal("50000.00")));
        assertFalse(budgetService.checkBudgetAvailability(10L, 2026, new BigDecimal("50000.01")));
    }

    @Test
    @DisplayName("Reserve budget succeeds and updates reserved and available balances")
    void testReserveBudget_Success() {
        when(budgetRepository.findByDepartmentIdAndFiscalYearForUpdate(10L, 2026)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(DepartmentBudget.class))).thenAnswer(inv -> inv.getArgument(0));

        BudgetResponse response = budgetService.reserveBudget(10L, 2026, new BigDecimal("15000.00"));

        assertNotNull(response);
        assertEquals(new BigDecimal("15000.00"), response.getReservedAmount());
        assertEquals(new BigDecimal("35000.00"), response.getAvailableAmount());
    }

    @Test
    @DisplayName("Reserve budget at exact boundary (100% of allocation)")
    void testReserveBudget_ExactBoundary() {
        when(budgetRepository.findByDepartmentIdAndFiscalYearForUpdate(10L, 2026)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(DepartmentBudget.class))).thenAnswer(inv -> inv.getArgument(0));

        BudgetResponse response = budgetService.reserveBudget(10L, 2026, new BigDecimal("50000.00"));

        assertNotNull(response);
        assertEquals(new BigDecimal("50000.00"), response.getReservedAmount());
        assertEquals(new BigDecimal("0.00"), response.getAvailableAmount());
    }

    @Test
    @DisplayName("Reserve budget throws BadRequestException when insufficient funds")
    void testReserveBudget_InsufficientFunds() {
        when(budgetRepository.findByDepartmentIdAndFiscalYearForUpdate(10L, 2026)).thenReturn(Optional.of(budget));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                budgetService.reserveBudget(10L, 2026, new BigDecimal("50000.01")));

        assertTrue(ex.getMessage().contains("Insufficient budget"));
        verify(budgetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reserve budget throws BadRequestException for zero or negative amounts")
    void testReserveBudget_InvalidAmounts() {
        assertThrows(BadRequestException.class, () ->
                budgetService.reserveBudget(10L, 2026, BigDecimal.ZERO));
        assertThrows(BadRequestException.class, () ->
                budgetService.reserveBudget(10L, 2026, new BigDecimal("-10.00")));
    }

    @Test
    @DisplayName("Release budget releases hold and restores available balance")
    void testReleaseBudget_Success() {
        budget.reserve(new BigDecimal("20000.00"));
        assertEquals(new BigDecimal("30000.00"), budget.getAvailableAmount());

        when(budgetRepository.findByDepartmentIdAndFiscalYearForUpdate(10L, 2026)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(DepartmentBudget.class))).thenAnswer(inv -> inv.getArgument(0));

        BudgetResponse response = budgetService.releaseBudget(10L, 2026, new BigDecimal("20000.00"));

        assertNotNull(response);
        assertEquals(0, response.getReservedAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.getAvailableAmount().compareTo(new BigDecimal("50000.00")));
    }

    @Test
    @DisplayName("Commit spend converts reserved hold into actual expense")
    void testCommitSpend_Success() {
        budget.reserve(new BigDecimal("12000.00"));

        when(budgetRepository.findByDepartmentIdAndFiscalYearForUpdate(10L, 2026)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(DepartmentBudget.class))).thenAnswer(inv -> inv.getArgument(0));

        BudgetResponse response = budgetService.commitSpend(10L, 2026, new BigDecimal("12000.00"));

        assertNotNull(response);
        assertEquals(0, response.getReservedAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.getSpentAmount().compareTo(new BigDecimal("12000.00")));
        assertEquals(0, response.getAvailableAmount().compareTo(new BigDecimal("38000.00")));
    }
}
