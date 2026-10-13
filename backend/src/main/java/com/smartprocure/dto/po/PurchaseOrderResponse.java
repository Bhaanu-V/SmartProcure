package com.smartprocure.dto.po;

import com.smartprocure.entity.POStatus;
import com.smartprocure.entity.Priority;
import com.smartprocure.entity.PurchaseOrder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Detailed response payload representing an issued Purchase Order.
 */
public class PurchaseOrderResponse {

    private Long id;
    private String poNumber;
    private String title;
    private String description;
    private String vendorName;
    private Long supplierId;
    private String supplierName;
    private Long requestId;
    private String requestNumber;
    private Long departmentId;
    private String departmentName;
    private Long requesterId;
    private String requesterName;
    private String currency;
    private BigDecimal totalAmount;
    private POStatus status;
    private Priority priority;
    private BigDecimal aiRiskScore;
    private String aiRecommendation;
    private String aiAnalysisSummary;
    private List<PurchaseOrderItemResponse> lineItems = new ArrayList<>();
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public PurchaseOrderResponse() {
    }

    public static PurchaseOrderResponse fromEntity(PurchaseOrder po) {
        PurchaseOrderResponse resp = new PurchaseOrderResponse();
        resp.setId(po.getId());
        resp.setPoNumber(po.getPoNumber());
        resp.setTitle(po.getTitle());
        resp.setDescription(po.getDescription());
        resp.setVendorName(po.getVendorName());

        if (po.getSupplier() != null) {
            resp.setSupplierId(po.getSupplier().getId());
            resp.setSupplierName(po.getSupplier().getName());
        }
        if (po.getPurchaseRequest() != null) {
            resp.setRequestId(po.getPurchaseRequest().getId());
            resp.setRequestNumber(po.getPurchaseRequest().getRequestNumber());
        }
        if (po.getDepartment() != null) {
            resp.setDepartmentId(po.getDepartment().getId());
            resp.setDepartmentName(po.getDepartment().getName());
        }
        if (po.getRequester() != null) {
            resp.setRequesterId(po.getRequester().getId());
            resp.setRequesterName(po.getRequester().getFullName());
        }
        resp.setCurrency(po.getCurrency());
        resp.setTotalAmount(po.getTotalAmount());
        resp.setStatus(po.getStatus());
        resp.setPriority(po.getPriority());
        resp.setAiRiskScore(po.getAiRiskScore());
        resp.setAiRecommendation(po.getAiRecommendation());
        resp.setAiAnalysisSummary(po.getAiAnalysisSummary());

        if (po.getLineItems() != null) {
            resp.setLineItems(po.getLineItems().stream()
                    .map(PurchaseOrderItemResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        resp.setCreatedAt(po.getCreatedAt());
        resp.setUpdatedAt(po.getUpdatedAt());
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
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

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public POStatus getStatus() {
        return status;
    }

    public void setStatus(POStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public BigDecimal getAiRiskScore() {
        return aiRiskScore;
    }

    public void setAiRiskScore(BigDecimal aiRiskScore) {
        this.aiRiskScore = aiRiskScore;
    }

    public String getAiRecommendation() {
        return aiRecommendation;
    }

    public void setAiRecommendation(String aiRecommendation) {
        this.aiRecommendation = aiRecommendation;
    }

    public String getAiAnalysisSummary() {
        return aiAnalysisSummary;
    }

    public void setAiAnalysisSummary(String aiAnalysisSummary) {
        this.aiAnalysisSummary = aiAnalysisSummary;
    }

    public List<PurchaseOrderItemResponse> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<PurchaseOrderItemResponse> lineItems) {
        this.lineItems = lineItems;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
