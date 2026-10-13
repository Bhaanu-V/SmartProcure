package com.smartprocure.service;

import com.smartprocure.dto.supplier.CreateSupplierRequest;
import com.smartprocure.dto.supplier.SupplierResponse;
import com.smartprocure.dto.supplier.UpdateSupplierRequest;
import com.smartprocure.entity.Supplier;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing supplier / vendor lifecycle, onboarding, and profile maintenance.
 */
@Service
public class SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierService.class);

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        if (supplierRepository.existsByName(request.getName().trim())) {
            throw new BadRequestException("Supplier with name '" + request.getName().trim() + "' already exists");
        }

        Supplier supplier = new Supplier();
        supplier.setName(request.getName().trim());
        supplier.setContactEmail(request.getContactEmail().trim().toLowerCase());
        supplier.setContactPhone(request.getContactPhone() != null ? request.getContactPhone().trim() : null);
        supplier.setTaxId(request.getTaxId() != null ? request.getTaxId().trim() : null);
        supplier.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        supplier.setPaymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms().trim() : "NET_30");
        if (request.getRating() != null) {
            supplier.setRating(request.getRating());
        }
        supplier.setActive(true);

        Supplier saved = supplierRepository.save(supplier);
        log.info("Created new supplier: '{}' (ID: {})", saved.getName(), saved.getId());
        return SupplierResponse.fromEntity(saved);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(supplier.getName()) && supplierRepository.existsByName(newName)) {
                throw new BadRequestException("Supplier with name '" + newName + "' already exists");
            }
            supplier.setName(newName);
        }

        if (request.getContactEmail() != null && !request.getContactEmail().trim().isEmpty()) {
            supplier.setContactEmail(request.getContactEmail().trim().toLowerCase());
        }
        if (request.getContactPhone() != null) {
            supplier.setContactPhone(request.getContactPhone().trim());
        }
        if (request.getTaxId() != null) {
            supplier.setTaxId(request.getTaxId().trim());
        }
        if (request.getAddress() != null) {
            supplier.setAddress(request.getAddress().trim());
        }
        if (request.getPaymentTerms() != null && !request.getPaymentTerms().trim().isEmpty()) {
            supplier.setPaymentTerms(request.getPaymentTerms().trim());
        }
        if (request.getRating() != null) {
            supplier.setRating(request.getRating());
        }
        if (request.getActive() != null) {
            supplier.setActive(request.getActive());
        }

        Supplier saved = supplierRepository.save(supplier);
        log.info("Updated supplier profile: '{}' (ID: {})", saved.getName(), saved.getId());
        return SupplierResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
        return SupplierResponse.fromEntity(supplier);
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> getAllSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable)
                .map(SupplierResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getActiveSuppliers() {
        return supplierRepository.findByActiveTrue().stream()
                .map(SupplierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> searchSuppliers(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return getAllSuppliers(pageable);
        }
        return supplierRepository.searchSuppliers(query.trim(), pageable)
                .map(SupplierResponse::fromEntity);
    }

    @Transactional
    public SupplierResponse setSupplierActiveStatus(Long id, boolean active) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
        supplier.setActive(active);
        Supplier saved = supplierRepository.save(supplier);
        log.info("Set supplier '{}' (ID: {}) active status to {}", saved.getName(), saved.getId(), active);
        return SupplierResponse.fromEntity(saved);
    }
}
