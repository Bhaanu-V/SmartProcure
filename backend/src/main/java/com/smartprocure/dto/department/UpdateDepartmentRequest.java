package com.smartprocure.dto.department;

import jakarta.validation.constraints.Size;

public class UpdateDepartmentRequest {

    @Size(max = 150, message = "Department name must not exceed 150 characters")
    private String name;

    private String description;

    private Long managerId;

    private Boolean active;

    public UpdateDepartmentRequest() {
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

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
