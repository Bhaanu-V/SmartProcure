package com.smartprocure.dto.pr;

import com.smartprocure.entity.PurchaseRequestItem;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class LineItemResponse {

    private Long id;
    private Long catalogItemId;
    private String itemDescription;
    private String category;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private OffsetDateTime createdAt;

    public LineItemResponse() {
    }

    public LineItemResponse(Long id,
                            Long catalogItemId,
                            String itemDescription,
                            String category,
                            BigDecimal quantity,
                            BigDecimal unitPrice,
                            BigDecimal totalPrice,
                            OffsetDateTime createdAt) {
        this.id = id;
        this.catalogItemId = catalogItemId;
        this.itemDescription = itemDescription;
        this.category = category;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
    }

    public static LineItemResponse fromEntity(PurchaseRequestItem item) {
        if (item == null) {
            return null;
        }
        Long catalogId = item.getCatalogItem() != null ? item.getCatalogItem().getId() : null;
        return new LineItemResponse(
                item.getId(),
                catalogId,
                item.getItemDescription(),
                item.getCategory(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
