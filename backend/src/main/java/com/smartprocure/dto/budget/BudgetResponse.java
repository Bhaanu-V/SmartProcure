package com.smartprocure.dto.budget;

import com.smartprocure.entity.DepartmentBudget;

import java.math.BigDecimal;

/**
 * DTO representing department budget details and real-time available balance.
 */
public class BudgetResponse {

    private Long id;
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private int fiscalYear;
    private BigDecimal allocatedAmount;
    private BigDecimal reservedAmount;
    private BigDecimal spentAmount;
    private BigDecimal availableAmount;

    public BudgetResponse() {
    }

    public BudgetResponse(Long id,
                          Long departmentId,
                          String departmentName,
                          String departmentCode,
                          int fiscalYear,
                          BigDecimal allocatedAmount,
                          BigDecimal reservedAmount,
                          BigDecimal spentAmount,
                          BigDecimal availableAmount) {
        this.id = id;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.fiscalYear = fiscalYear;
        this.allocatedAmount = allocatedAmount;
        this.reservedAmount = reservedAmount;
        this.spentAmount = spentAmount;
        this.availableAmount = availableAmount;
    }

    public static BudgetResponse fromEntity(DepartmentBudget budget) {
        String deptName = budget.getDepartment() != null ? budget.getDepartment().getName() : null;
        String deptCode = budget.getDepartment() != null ? budget.getDepartment().getCode() : null;
        Long deptId = budget.getDepartment() != null ? budget.getDepartment().getId() : null;

        return new BudgetResponse(
                budget.getId(),
                deptId,
                deptName,
                deptCode,
                budget.getFiscalYear(),
                budget.getAllocatedAmount(),
                budget.getReservedAmount(),
                budget.getSpentAmount(),
                budget.getAvailableAmount()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
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

    public BigDecimal getReservedAmount() {
        return reservedAmount;
    }

    public void setReservedAmount(BigDecimal reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }

    public void setAvailableAmount(BigDecimal availableAmount) {
        this.availableAmount = availableAmount;
    }
}
