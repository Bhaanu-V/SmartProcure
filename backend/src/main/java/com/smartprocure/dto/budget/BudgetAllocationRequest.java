package com.smartprocure.dto.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BudgetAllocationRequest {

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @Min(value = 2020, message = "Fiscal year must be 2020 or later")
    private int fiscalYear;

    @NotNull(message = "Allocated amount is required")
    @DecimalMin(value = "0.01", message = "Allocated amount must be greater than zero")
    private BigDecimal allocatedAmount;

    public BudgetAllocationRequest() {
    }

    public BudgetAllocationRequest(Long departmentId, int fiscalYear, BigDecimal allocatedAmount) {
        this.departmentId = departmentId;
        this.fiscalYear = fiscalYear;
        this.allocatedAmount = allocatedAmount;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(int fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }
}
