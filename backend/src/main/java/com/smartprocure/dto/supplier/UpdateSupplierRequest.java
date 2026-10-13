package com.smartprocure.dto.supplier;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdateSupplierRequest {

    @Size(max = 255, message = "Supplier name must not exceed 255 characters")
    private String name;

    @Email(message = "Contact email must be a valid email format")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    private String contactEmail;

    @Size(max = 50, message = "Contact phone must not exceed 50 characters")
    private String contactPhone;

    @Size(max = 50, message = "Tax ID must not exceed 50 characters")
    private String taxId;

    private String address;

    @Size(max = 50, message = "Payment terms must not exceed 50 characters")
    private String paymentTerms;

    @DecimalMin(value = "0.00", message = "Rating must be at least 0.00")
    @DecimalMax(value = "5.00", message = "Rating must not exceed 5.00")
    private BigDecimal rating;

    private Boolean active;

    public UpdateSupplierRequest() {
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
