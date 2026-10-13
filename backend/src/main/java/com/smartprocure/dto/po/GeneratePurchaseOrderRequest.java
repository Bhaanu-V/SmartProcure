package com.smartprocure.dto.po;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for issuing a Purchase Order from an approved Purchase Request.
 */
public class GeneratePurchaseOrderRequest {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @Size(max = 1000, message = "Delivery notes must not exceed 1000 characters")
    private String deliveryNotes;

    public GeneratePurchaseOrderRequest() {
    }

    public GeneratePurchaseOrderRequest(Long supplierId, String deliveryNotes) {
        this.supplierId = supplierId;
        this.deliveryNotes = deliveryNotes;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }
}
