package com.smartprocure.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Department budget entity with optimistic locking and reservation tracking.
 */
@Entity
@Table(
    name = "department_budgets",
    schema = "smartprocure",
    uniqueConstraints = @UniqueConstraint(name = "uq_dept_fiscal_year", columnNames = {"department_id", "fiscal_year"})
)
public class DepartmentBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "allocated_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(name = "spent_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(name = "reserved_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal reservedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public DepartmentBudget() {
    }

    public DepartmentBudget(Department department, Integer fiscalYear, BigDecimal allocatedAmount) {
        this.department = department;
        this.fiscalYear = fiscalYear;
        this.allocatedAmount = allocatedAmount != null ? allocatedAmount : BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Calculates currently available unreserved budget.
     */
    public BigDecimal getAvailableAmount() {
        return allocatedAmount.subtract(spentAmount).subtract(reservedAmount);
    }

    /**
     * Checks if requested amount can be safely reserved without budget deficit.
     */
    public boolean canReserve(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return getAvailableAmount().compareTo(amount) >= 0;
    }

    /**
     * Places a hold on budget for pending purchase request.
     */
    public void reserve(BigDecimal amount) {
        if (!canReserve(amount)) {
            throw new IllegalStateException("Insufficient available budget to reserve: " + amount);
        }
        this.reservedAmount = this.reservedAmount.add(amount);
    }

    /**
     * Releases hold when a request is rejected or cancelled.
     */
    public void releaseReservation(BigDecimal amount) {
        if (amount != null && this.reservedAmount.compareTo(amount) >= 0) {
            this.reservedAmount = this.reservedAmount.subtract(amount);
        } else {
            this.reservedAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Commits reserved funds as actual spent amount upon PO issuance.
     */
    public void commitExpense(BigDecimal amount) {
        releaseReservation(amount);
        this.spentAmount = this.spentAmount.add(amount);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public BigDecimal getReservedAmount() {
        return reservedAmount;
    }

    public void setReservedAmount(BigDecimal reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
