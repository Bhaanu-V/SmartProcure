package com.smartprocure.dto.approval;

import com.smartprocure.entity.ApprovalAction;
import com.smartprocure.entity.ApprovalHistory;
import com.smartprocure.entity.ApprovalStage;

import java.time.OffsetDateTime;

/**
 * Response payload representing an immutable approval or rejection decision.
 */
public class ApprovalHistoryResponse {

    private Long id;
    private Long requestId;
    private String requestNumber;
    private Long orderId;
    private String orderNumber;
    private Long actorId;
    private String actorName;
    private String actorEmail;
    private ApprovalAction action;
    private ApprovalStage stage;
    private String comments;
    private OffsetDateTime createdAt;

    public ApprovalHistoryResponse() {
    }

    public static ApprovalHistoryResponse fromEntity(ApprovalHistory history) {
        ApprovalHistoryResponse resp = new ApprovalHistoryResponse();
        resp.setId(history.getId());
        if (history.getPurchaseRequest() != null) {
            resp.setRequestId(history.getPurchaseRequest().getId());
            resp.setRequestNumber(history.getPurchaseRequest().getRequestNumber());
        }
        if (history.getPurchaseOrder() != null) {
            resp.setOrderId(history.getPurchaseOrder().getId());
            resp.setOrderNumber(history.getPurchaseOrder().getPoNumber());
        }
        if (history.getActor() != null) {
            resp.setActorId(history.getActor().getId());
            resp.setActorName(history.getActor().getFullName());
            resp.setActorEmail(history.getActor().getEmail());
        }
        resp.setAction(history.getAction());
        resp.setStage(history.getStage());
        resp.setComments(history.getComments());
        resp.setCreatedAt(history.getCreatedAt());
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public ApprovalAction getAction() {
        return action;
    }

    public void setAction(ApprovalAction action) {
        this.action = action;
    }

    public ApprovalStage getStage() {
        return stage;
    }

    public void setStage(ApprovalStage stage) {
        this.stage = stage;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
