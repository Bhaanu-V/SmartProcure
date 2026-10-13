package com.smartprocure.dto.pr;

import com.smartprocure.entity.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class CreatePurchaseRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private Priority priority = Priority.MEDIUM;

    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency = "USD";

    @Valid
    @NotEmpty(message = "At least one line item is required")
    private List<CreateLineItemRequest> lineItems = new ArrayList<>();

    public CreatePurchaseRequest() {
    }

    public CreatePurchaseRequest(String title, Long departmentId, List<CreateLineItemRequest> lineItems) {
        this.title = title;
        this.departmentId = departmentId;
        this.lineItems = lineItems;
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<CreateLineItemRequest> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<CreateLineItemRequest> lineItems) {
        this.lineItems = lineItems;
    }
}
