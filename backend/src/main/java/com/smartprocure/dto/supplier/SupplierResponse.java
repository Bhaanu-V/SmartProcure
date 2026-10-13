package com.smartprocure.dto.supplier;

import com.smartprocure.entity.Supplier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class SupplierResponse {

    private Long id;
    private String name;
    private String contactEmail;
    private String contactPhone;
    private String taxId;
    private String address;
    private String paymentTerms;
    private BigDecimal rating;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public SupplierResponse() {
    }

    public SupplierResponse(Long id,
                            String name,
                            String contactEmail,
                            String contactPhone,
                            String taxId,
                            String address,
                            String paymentTerms,
                            BigDecimal rating,
                            boolean active,
                            OffsetDateTime createdAt,
                            OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.taxId = taxId;
        this.address = address;
        this.paymentTerms = paymentTerms;
        this.rating = rating;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SupplierResponse fromEntity(Supplier supplier) {
        if (supplier == null) {
            return null;
        }
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getContactEmail(),
                supplier.getContactPhone(),
                supplier.getTaxId(),
                supplier.getAddress(),
                supplier.getPaymentTerms(),
                supplier.getRating(),
                supplier.isActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
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
