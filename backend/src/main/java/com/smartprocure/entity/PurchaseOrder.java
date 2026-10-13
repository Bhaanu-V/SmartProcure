package com.smartprocure.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase Order entity representing an approved and issued supplier order.
 */
@Entity
@Table(name = "purchase_orders", schema = "smartprocure")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", nullable = false, unique = true, length = 64)
    private String poNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "vendor_name", nullable = false, length = 255)
    private String vendorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private POStatus status = POStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "ai_risk_score", precision = 5, scale = 2)
    private BigDecimal aiRiskScore;

    @Column(name = "ai_recommendation", length = 50)
    private String aiRecommendation;

    @Column(name = "ai_analysis_summary", columnDefinition = "TEXT")
    private String aiAnalysisSummary;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> lineItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public PurchaseOrder() {
    }

    public PurchaseOrder(String poNumber, String title, String vendorName, BigDecimal totalAmount) {
        this.poNumber = poNumber;
        this.title = title;
        this.vendorName = vendorName;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        recalculateTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
        recalculateTotal();
    }

    public void addLineItem(PurchaseOrderItem item) {
        lineItems.add(item);
        item.setPurchaseOrder(this);
        recalculateTotal();
    }

    public void removeLineItem(PurchaseOrderItem item) {
        lineItems.remove(item);
        item.setPurchaseOrder(null);
        recalculateTotal();
    }

    public void recalculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        if (lineItems != null) {
            for (PurchaseOrderItem item : lineItems) {
                if (item.getTotalPrice() != null) {
                    sum = sum.add(item.getTotalPrice());
                }
            }
        }
        if (sum.compareTo(BigDecimal.ZERO) > 0) {
            this.totalAmount = sum;
        }
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

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
        if (supplier != null && (this.vendorName == null || this.vendorName.isBlank())) {
            this.vendorName = supplier.getName();
        }
    }

    public PurchaseRequest getPurchaseRequest() {
        return purchaseRequest;
    }

    public void setPurchaseRequest(PurchaseRequest purchaseRequest) {
        this.purchaseRequest = purchaseRequest;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
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

    public List<PurchaseOrderItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<PurchaseOrderItem> lineItems) {
        this.lineItems = lineItems;
        recalculateTotal();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
