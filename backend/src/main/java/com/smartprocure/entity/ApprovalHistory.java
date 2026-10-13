package com.smartprocure.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Immutable audit record capturing an approval or rejection decision.
 */
@Entity
@Table(name = "approval_history", schema = "smartprocure")
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApprovalAction action;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ApprovalStage stage;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ApprovalHistory() {
    }

    public ApprovalHistory(PurchaseRequest purchaseRequest, User actor, ApprovalAction action, ApprovalStage stage, String comments) {
        this.purchaseRequest = purchaseRequest;
        this.actor = actor;
        this.action = action;
        this.stage = stage;
        this.comments = comments;
    }

    public ApprovalHistory(PurchaseOrder purchaseOrder, User actor, ApprovalAction action, String comments) {
        this.purchaseOrder = purchaseOrder;
        this.actor = actor;
        this.action = action;
        this.comments = comments;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public PurchaseRequest getPurchaseRequest() {
        return purchaseRequest;
    }

    public void setPurchaseRequest(PurchaseRequest purchaseRequest) {
        this.purchaseRequest = purchaseRequest;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
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
}
