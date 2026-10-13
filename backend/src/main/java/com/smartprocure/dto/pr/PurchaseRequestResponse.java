package com.smartprocure.dto.pr;

import com.smartprocure.entity.Priority;
import com.smartprocure.entity.PurchaseRequest;
import com.smartprocure.entity.RequestStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PurchaseRequestResponse {

    private Long id;
    private String requestNumber;
    private String title;
    private String description;
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private Long requesterId;
    private String requesterName;
    private String requesterEmail;
    private BigDecimal totalAmount;
    private String currency;
    private RequestStatus status;
    private Priority priority;
    private String rejectionReason;
    private BigDecimal aiRiskScore;
    private String aiRecommendation;
    private String aiAnalysisSummary;
    private List<LineItemResponse> lineItems;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public PurchaseRequestResponse() {
    }

    public static PurchaseRequestResponse fromEntity(PurchaseRequest request) {
        if (request == null) {
            return null;
        }

        PurchaseRequestResponse res = new PurchaseRequestResponse();
        res.id = request.getId();
        res.requestNumber = request.getRequestNumber();
        res.title = request.getTitle();
        res.description = request.getDescription();

        if (request.getDepartment() != null) {
            res.departmentId = request.getDepartment().getId();
            res.departmentName = request.getDepartment().getName();
            res.departmentCode = request.getDepartment().getCode();
        }

        if (request.getRequester() != null) {
            res.requesterId = request.getRequester().getId();
            res.requesterName = request.getRequester().getFullName();
            res.requesterEmail = request.getRequester().getEmail();
        }

        res.totalAmount = request.getTotalAmount();
        res.currency = request.getCurrency();
        res.status = request.getStatus();
        res.priority = request.getPriority();
        res.rejectionReason = request.getRejectionReason();
        res.aiRiskScore = request.getAiRiskScore();
        res.aiRecommendation = request.getAiRecommendation();
        res.aiAnalysisSummary = request.getAiAnalysisSummary();
        res.createdAt = request.getCreatedAt();
        res.updatedAt = request.getUpdatedAt();

        if (request.getLineItems() != null) {
            res.lineItems = request.getLineItems().stream()
                    .map(LineItemResponse::fromEntity)
                    .collect(Collectors.toList());
        } else {
            res.lineItems = Collections.emptyList();
        }

        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
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

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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

    public List<LineItemResponse> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<LineItemResponse> lineItems) {
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
