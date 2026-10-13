package com.smartprocure.dto.po;

import com.smartprocure.entity.PurchaseOrderItem;

import java.math.BigDecimal;

/**
 * Line item response payload for Purchase Orders.
 */
public class PurchaseOrderItemResponse {

    private Long id;
    private String itemDescription;
    private String category;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    public PurchaseOrderItemResponse() {
    }

    public static PurchaseOrderItemResponse fromEntity(PurchaseOrderItem item) {
        PurchaseOrderItemResponse resp = new PurchaseOrderItemResponse();
        resp.setId(item.getId());
        resp.setItemDescription(item.getItemDescription());
        resp.setCategory(item.getCategory());
        resp.setQuantity(item.getQuantity());
        resp.setUnitPrice(item.getUnitPrice());
        resp.setTotalPrice(item.getTotalPrice());
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
