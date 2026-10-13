package com.smartprocure.service;

import com.smartprocure.dto.department.CreateDepartmentRequest;
import com.smartprocure.dto.department.DepartmentResponse;
import com.smartprocure.dto.department.UpdateDepartmentRequest;
import com.smartprocure.entity.Department;
import com.smartprocure.entity.User;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.DepartmentRepository;
import com.smartprocure.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing organizational departments, departmental managers, and active status.
 */
@Service
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (departmentRepository.existsByCode(code)) {
            throw new BadRequestException("Department with code '" + code + "' already exists");
        }

        String name = request.getName().trim();
        if (departmentRepository.existsByName(name)) {
            throw new BadRequestException("Department with name '" + name + "' already exists");
        }

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getManagerId()));
        }

        Department dept = new Department();
        dept.setCode(code);
        dept.setName(name);
        dept.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        dept.setManager(manager);
        dept.setActive(true);

        Department saved = departmentRepository.save(dept);
        log.info("Created new department '{}' (Code: {}, ID: {})", saved.getName(), saved.getCode(), saved.getId());
        return DepartmentResponse.fromEntity(saved);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(dept.getName()) && departmentRepository.existsByName(newName)) {
                throw new BadRequestException("Department with name '" + newName + "' already exists");
            }
            dept.setName(newName);
        }

        if (request.getDescription() != null) {
            dept.setDescription(request.getDescription().trim());
        }

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getManagerId()));
            dept.setManager(manager);
        }

        if (request.getActive() != null) {
            dept.setActive(request.getActive());
        }

        Department saved = departmentRepository.save(dept);
        log.info("Updated department '{}' (ID: {})", saved.getName(), saved.getId());
        return DepartmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return DepartmentResponse.fromEntity(dept);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentByCode(String code) {
        Department dept = departmentRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "code", code));
        return DepartmentResponse.fromEntity(dept);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(DepartmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DepartmentResponse setDepartmentActiveStatus(Long id, boolean active) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        dept.setActive(active);
        Department saved = departmentRepository.save(dept);
        log.info("Set department '{}' active status to {}", saved.getName(), active);
        return DepartmentResponse.fromEntity(saved);
    }
}
