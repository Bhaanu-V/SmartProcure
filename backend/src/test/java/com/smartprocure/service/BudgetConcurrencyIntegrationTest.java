package com.smartprocure.service;

import com.smartprocure.dto.budget.BudgetAllocationRequest;
import com.smartprocure.dto.budget.BudgetResponse;
import com.smartprocure.entity.Department;
import com.smartprocure.entity.DepartmentBudget;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.repository.DepartmentBudgetRepository;
import com.smartprocure.repository.DepartmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class BudgetConcurrencyIntegrationTest {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentBudgetRepository budgetRepository;

    private Department testDept;
    private final int fiscalYear = 2026;

    @BeforeEach
    void setUp() {
        testDept = new Department("TEST_CONC", "Concurrency Test Dept", "Isolated test dept");
        testDept = departmentRepository.save(testDept);

        BudgetAllocationRequest alloc = new BudgetAllocationRequest(
                testDept.getId(), fiscalYear, new BigDecimal("10000.00"));
        budgetService.allocateBudget(alloc);
    }

    @AfterEach
    void tearDown() {
        if (testDept != null && testDept.getId() != null) {
            budgetRepository.findByDepartmentIdAndFiscalYear(testDept.getId(), fiscalYear)
                    .ifPresent(b -> budgetRepository.delete(b));
            departmentRepository.delete(testDept);
        }
    }

    @Test
    @DisplayName("Pessimistic lock prevents race condition during concurrent budget reservations")
    void testConcurrentBudgetReservations() throws InterruptedException {
        int threadCount = 5;
        BigDecimal requestAmount = new BigDecimal("3000.00"); // 5 * 3000 = 15000, but budget is only 10000

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // ensure all threads launch simultaneously
                    budgetService.reserveBudget(testDept.getId(), fiscalYear, requestAmount);
                    successCount.incrementAndGet();
                } catch (BadRequestException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // unblock all threads simultaneously
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(true, completed, "All concurrent tasks should complete within timeout");
        assertEquals(3, successCount.get(), "Exactly 3 reservations of $3,000 should succeed for a $10,000 budget");
        assertEquals(2, failureCount.get(), "Exactly 2 reservations should fail due to insufficient budget");

        BudgetResponse finalBudget = budgetService.getBudget(testDept.getId(), fiscalYear);
        assertEquals(new BigDecimal("9000.00"), finalBudget.getReservedAmount());
        assertEquals(new BigDecimal("1000.00"), finalBudget.getAvailableAmount());
    }
}
