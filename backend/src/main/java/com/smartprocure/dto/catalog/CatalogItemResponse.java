package com.smartprocure.dto.catalog;

import com.smartprocure.entity.CatalogItem;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class CatalogItemResponse {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private String category;
    private BigDecimal unitPrice;
    private String currency;
    private Long supplierId;
    private String supplierName;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public CatalogItemResponse() {
    }

    public CatalogItemResponse(Long id,
                               String sku,
                               String name,
                               String description,
                               String category,
                               BigDecimal unitPrice,
                               String currency,
                               Long supplierId,
                               String supplierName,
                               boolean active,
                               OffsetDateTime createdAt,
                               OffsetDateTime updatedAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.unitPrice = unitPrice;
        this.currency = currency;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CatalogItemResponse fromEntity(CatalogItem item) {
        if (item == null) {
            return null;
        }
        Long supplierId = item.getSupplier() != null ? item.getSupplier().getId() : null;
        String supplierName = item.getSupplier() != null ? item.getSupplier().getName() : null;

        return new CatalogItemResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getDescription(),
                item.getCategory(),
                item.getUnitPrice(),
                item.getCurrency(),
                supplierId,
                supplierName,
                item.isActive(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
