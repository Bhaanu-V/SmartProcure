package com.smartprocure.dto.department;

import com.smartprocure.entity.Department;

import java.time.OffsetDateTime;

public class DepartmentResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Long managerId;
    private String managerName;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public DepartmentResponse() {
    }

    public DepartmentResponse(Long id,
                              String code,
                              String name,
                              String description,
                              Long managerId,
                              String managerName,
                              boolean active,
                              OffsetDateTime createdAt,
                              OffsetDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.managerId = managerId;
        this.managerName = managerName;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DepartmentResponse fromEntity(Department dept) {
        if (dept == null) {
            return null;
        }
        Long mgrId = dept.getManager() != null ? dept.getManager().getId() : null;
        String mgrName = dept.getManager() != null ? dept.getManager().getFullName() : null;

        return new DepartmentResponse(
                dept.getId(),
                dept.getCode(),
                dept.getName(),
                dept.getDescription(),
                mgrId,
                mgrName,
                dept.isActive(),
                dept.getCreatedAt(),
                dept.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
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
