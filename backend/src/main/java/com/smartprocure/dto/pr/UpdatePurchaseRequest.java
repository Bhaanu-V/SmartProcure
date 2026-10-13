package com.smartprocure.dto.pr;

import com.smartprocure.entity.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UpdatePurchaseRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    private Long departmentId;

    private Priority priority;

    @Valid
    private List<CreateLineItemRequest> lineItems;

    public UpdatePurchaseRequest() {
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

    public List<CreateLineItemRequest> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<CreateLineItemRequest> lineItems) {
        this.lineItems = lineItems;
    }
}
