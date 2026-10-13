package com.smartprocure.service;

import com.smartprocure.dto.catalog.CatalogItemResponse;
import com.smartprocure.dto.catalog.CreateCatalogItemRequest;
import com.smartprocure.dto.catalog.UpdateCatalogItemRequest;
import com.smartprocure.entity.CatalogItem;
import com.smartprocure.entity.Supplier;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.CatalogItemRepository;
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
 * Service managing catalog items, product offerings, categories, and pre-negotiated pricing.
 */
@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final CatalogItemRepository catalogItemRepository;
    private final SupplierRepository supplierRepository;

    public CatalogService(CatalogItemRepository catalogItemRepository,
                          SupplierRepository supplierRepository) {
        this.catalogItemRepository = catalogItemRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public CatalogItemResponse createCatalogItem(CreateCatalogItemRequest request) {
        String normalizedSku = request.getSku().trim().toUpperCase();
        if (catalogItemRepository.existsBySku(normalizedSku)) {
            throw new BadRequestException("Catalog item with SKU '" + normalizedSku + "' already exists");
        }

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));
        }

        CatalogItem item = new CatalogItem();
        item.setSku(normalizedSku);
        item.setName(request.getName().trim());
        item.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        item.setCategory(request.getCategory().trim().toUpperCase());
        item.setUnitPrice(request.getUnitPrice());
        item.setCurrency(request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "USD");
        item.setSupplier(supplier);
        item.setActive(true);

        CatalogItem saved = catalogItemRepository.save(item);
        log.info("Created catalog item SKU '{}' (ID: {})", saved.getSku(), saved.getId());
        return CatalogItemResponse.fromEntity(saved);
    }

    @Transactional
    public CatalogItemResponse updateCatalogItem(Long id, UpdateCatalogItemRequest request) {
        CatalogItem item = catalogItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CatalogItem", "id", id));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            item.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            item.setCategory(request.getCategory().trim().toUpperCase());
        }
        if (request.getUnitPrice() != null) {
            item.setUnitPrice(request.getUnitPrice());
        }
        if (request.getCurrency() != null && !request.getCurrency().trim().isEmpty()) {
            item.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));
            item.setSupplier(supplier);
        }
        if (request.getActive() != null) {
            item.setActive(request.getActive());
        }

        CatalogItem saved = catalogItemRepository.save(item);
        log.info("Updated catalog item SKU '{}' (ID: {})", saved.getSku(), saved.getId());
        return CatalogItemResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public CatalogItemResponse getCatalogItemById(Long id) {
        CatalogItem item = catalogItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CatalogItem", "id", id));
        return CatalogItemResponse.fromEntity(item);
    }

    @Transactional(readOnly = true)
    public CatalogItemResponse getCatalogItemBySku(String sku) {
        CatalogItem item = catalogItemRepository.findBySku(sku.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("CatalogItem", "sku", sku));
        return CatalogItemResponse.fromEntity(item);
    }

    @Transactional(readOnly = true)
    public Page<CatalogItemResponse> getAllCatalogItems(Pageable pageable) {
        return catalogItemRepository.findAll(pageable)
                .map(CatalogItemResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CatalogItemResponse> searchCatalog(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return getAllCatalogItems(pageable);
        }
        return catalogItemRepository.searchCatalog(query.trim(), pageable)
                .map(CatalogItemResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getItemsBySupplier(Long supplierId) {
        return catalogItemRepository.findBySupplierIdAndActiveTrue(supplierId).stream()
                .map(CatalogItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getItemsByCategory(String category) {
        return catalogItemRepository.findByCategory(category.trim().toUpperCase()).stream()
                .map(CatalogItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CatalogItemResponse setItemActiveStatus(Long id, boolean active) {
        CatalogItem item = catalogItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CatalogItem", "id", id));
        item.setActive(active);
        CatalogItem saved = catalogItemRepository.save(item);
        log.info("Set catalog item SKU '{}' active status to {}", saved.getSku(), active);
        return CatalogItemResponse.fromEntity(saved);
    }
}
