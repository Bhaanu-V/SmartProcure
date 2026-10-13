package com.smartprocure.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Line item belonging to a Purchase Order.
 */
@Entity
@Table(name = "po_line_items", schema = "smartprocure")
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "item_description", nullable = false, length = 255)
    private String itemDescription;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public PurchaseOrderItem() {
    }

    public PurchaseOrderItem(String itemDescription, BigDecimal quantity, BigDecimal unitPrice) {
        this.itemDescription = itemDescription;
        this.quantity = quantity != null ? quantity : BigDecimal.ONE;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        recalculateTotal();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        recalculateTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        recalculateTotal();
    }

    public void recalculateTotal() {
        if (quantity != null && unitPrice != null) {
            this.totalPrice = quantity.multiply(unitPrice);
        } else {
            this.totalPrice = BigDecimal.ZERO;
        }
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

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        recalculateTotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        recalculateTotal();
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
