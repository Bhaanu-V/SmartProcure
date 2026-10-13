package com.smartprocure.dto.po;

import com.smartprocure.entity.POStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for transitioning the lifecycle state of a Purchase Order.
 */
public class UpdatePOStatusRequest {

    @NotNull(message = "PO Status is required")
    private POStatus status;

    @Size(max = 1000, message = "Status notes must not exceed 1000 characters")
    private String notes;

    public UpdatePOStatusRequest() {
    }

    public UpdatePOStatusRequest(POStatus status, String notes) {
        this.status = status;
        this.notes = notes;
    }

    public POStatus getStatus() {
        return status;
    }

    public void setStatus(POStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
