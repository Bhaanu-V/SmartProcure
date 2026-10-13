package com.smartprocure.dto.auth;

import com.smartprocure.entity.Role;
import com.smartprocure.entity.User;

public class UserProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private String department;
    private Long departmentId;
    private boolean active;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long id,
                               String username,
                               String email,
                               String fullName,
                               Role role,
                               String department,
                               Long departmentId,
                               boolean active) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.department = department;
        this.departmentId = departmentId;
        this.active = active;
    }

    public static UserProfileResponse fromUser(User user) {
        String deptName = user.getDepartmentEntity() != null ? user.getDepartmentEntity().getName() : user.getDepartment();
        Long deptId = user.getDepartmentEntity() != null ? user.getDepartmentEntity().getId() : null;

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                deptName,
                deptId,
                user.isActive()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
