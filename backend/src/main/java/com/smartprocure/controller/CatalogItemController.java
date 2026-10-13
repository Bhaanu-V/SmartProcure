package com.smartprocure.controller;

import com.smartprocure.dto.catalog.CatalogItemResponse;
import com.smartprocure.dto.catalog.CreateCatalogItemRequest;
import com.smartprocure.dto.catalog.UpdateCatalogItemRequest;
import com.smartprocure.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for catalog item lookup, procurement browsing, and administration.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogItemController {

    private final CatalogService catalogService;

    public CatalogItemController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT')")
    public ResponseEntity<CatalogItemResponse> createCatalogItem(
            @Valid @RequestBody CreateCatalogItemRequest request) {
        CatalogItemResponse response = catalogService.createCatalogItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT')")
    public ResponseEntity<CatalogItemResponse> updateCatalogItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCatalogItemRequest request) {
        CatalogItemResponse response = catalogService.updateCatalogItem(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CatalogItemResponse> getCatalogItemById(@PathVariable Long id) {
        CatalogItemResponse response = catalogService.getCatalogItemById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sku/{sku}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CatalogItemResponse> getCatalogItemBySku(@PathVariable String sku) {
        CatalogItemResponse response = catalogService.getCatalogItemBySku(sku);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CatalogItemResponse>> getAllCatalogItems(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<CatalogItemResponse> response = catalogService.getAllCatalogItems(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CatalogItemResponse>> searchCatalog(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<CatalogItemResponse> response = catalogService.searchCatalog(query, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CatalogItemResponse>> getItemsBySupplier(@PathVariable Long supplierId) {
        List<CatalogItemResponse> response = catalogService.getItemsBySupplier(supplierId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CatalogItemResponse>> getItemsByCategory(@PathVariable String category) {
        List<CatalogItemResponse> response = catalogService.getItemsByCategory(category);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT')")
    public ResponseEntity<CatalogItemResponse> setItemActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        CatalogItemResponse response = catalogService.setItemActiveStatus(id, active);
        return ResponseEntity.ok(response);
    }
}
