package com.smartprocure.service;

import com.smartprocure.dto.budget.BudgetAllocationRequest;
import com.smartprocure.dto.budget.BudgetResponse;
import com.smartprocure.entity.Department;
import com.smartprocure.entity.DepartmentBudget;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.DepartmentBudgetRepository;
import com.smartprocure.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing department budget allocations, real-time balances,
 * and concurrency-safe budget reservations using pessimistic database locks.
 */
@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);

    private final DepartmentBudgetRepository budgetRepository;
    private final DepartmentRepository departmentRepository;

    public BudgetService(DepartmentBudgetRepository budgetRepository,
                         DepartmentRepository departmentRepository) {
        this.budgetRepository = budgetRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Allocates or updates an annual department budget.
     */
    @Transactional
    public BudgetResponse allocateBudget(BudgetAllocationRequest request) {
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));

        DepartmentBudget budget = budgetRepository
                .findByDepartmentIdAndFiscalYear(request.getDepartmentId(), request.getFiscalYear())
                .orElseGet(() -> new DepartmentBudget(department, request.getFiscalYear(), BigDecimal.ZERO));

        budget.setAllocatedAmount(request.getAllocatedAmount());
        DepartmentBudget saved = budgetRepository.save(budget);

        log.info("Allocated budget for department '{}' ({}) FY {}: {}",
                department.getName(), department.getCode(), request.getFiscalYear(), request.getAllocatedAmount());

        return BudgetResponse.fromEntity(saved);
    }

    /**
     * Retrieves the budget for a specific department and fiscal year.
     */
    @Transactional(readOnly = true)
    public BudgetResponse getBudget(Long departmentId, int fiscalYear) {
        DepartmentBudget budget = budgetRepository.findByDepartmentIdAndFiscalYear(departmentId, fiscalYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DepartmentBudget", "departmentId and fiscalYear", departmentId + " / " + fiscalYear));
        return BudgetResponse.fromEntity(budget);
    }

    /**
     * Retrieves all historical and active budgets for a department.
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getDepartmentBudgets(Long departmentId) {
        return budgetRepository.findByDepartmentId(departmentId).stream()
                .map(BudgetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Checks if sufficient funds are available without acquiring a lock.
     */
    @Transactional(readOnly = true)
    public boolean checkBudgetAvailability(Long departmentId, int fiscalYear, BigDecimal amount) {
        return budgetRepository.findByDepartmentIdAndFiscalYear(departmentId, fiscalYear)
                .map(b -> b.getAvailableAmount().compareTo(amount) >= 0)
                .orElse(false);
    }

    /**
     * Concurrency-safe budget reservation using pessimistic write lock.
     * Prevents race conditions and overspending from concurrent purchase requests.
     */
    @Transactional
    public BudgetResponse reserveBudget(Long departmentId, int fiscalYear, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Reservation amount must be strictly positive");
        }

        DepartmentBudget budget = budgetRepository
                .findByDepartmentIdAndFiscalYearForUpdate(departmentId, fiscalYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DepartmentBudget", "departmentId and fiscalYear", departmentId + " / " + fiscalYear));

        if (budget.getAvailableAmount().compareTo(amount) < 0) {
            throw new BadRequestException(String.format(
                    "Insufficient budget for department '%s' in FY %d. Available: %s, Requested: %s",
                    budget.getDepartment().getName(), fiscalYear, budget.getAvailableAmount(), amount));
        }

        budget.reserve(amount);
        DepartmentBudget saved = budgetRepository.save(budget);

        log.info("Reserved {} from department '{}' FY {} budget. New available: {}",
                amount, budget.getDepartment().getName(), fiscalYear, saved.getAvailableAmount());

        return BudgetResponse.fromEntity(saved);
    }

    /**
     * Releases previously reserved budget (e.g. upon request rejection or cancellation).
     */
    @Transactional
    public BudgetResponse releaseBudget(Long departmentId, int fiscalYear, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Release amount must be strictly positive");
        }

        DepartmentBudget budget = budgetRepository
                .findByDepartmentIdAndFiscalYearForUpdate(departmentId, fiscalYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DepartmentBudget", "departmentId and fiscalYear", departmentId + " / " + fiscalYear));

        budget.releaseReservation(amount);
        DepartmentBudget saved = budgetRepository.save(budget);

        log.info("Released {} back to department '{}' FY {} budget. New available: {}",
                amount, budget.getDepartment().getName(), fiscalYear, saved.getAvailableAmount());

        return BudgetResponse.fromEntity(saved);
    }

    /**
     * Commits reserved funds as actual spent funds upon purchase order completion.
     */
    @Transactional
    public BudgetResponse commitSpend(Long departmentId, int fiscalYear, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Spend amount must be strictly positive");
        }

        DepartmentBudget budget = budgetRepository
                .findByDepartmentIdAndFiscalYearForUpdate(departmentId, fiscalYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DepartmentBudget", "departmentId and fiscalYear", departmentId + " / " + fiscalYear));

        budget.commitExpense(amount);
        DepartmentBudget saved = budgetRepository.save(budget);

        log.info("Committed spend of {} for department '{}' FY {}. Total spent: {}",
                amount, budget.getDepartment().getName(), fiscalYear, saved.getSpentAmount());

        return BudgetResponse.fromEntity(saved);
    }
}
