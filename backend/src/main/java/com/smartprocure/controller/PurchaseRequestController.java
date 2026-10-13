package com.smartprocure.controller;

import com.smartprocure.dto.pr.CreatePurchaseRequest;
import com.smartprocure.dto.pr.PurchaseRequestResponse;
import com.smartprocure.dto.pr.UpdatePurchaseRequest;
import com.smartprocure.entity.RequestStatus;
import com.smartprocure.service.PurchaseRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for purchase requisition draft creation, lifecycle transitions, and role-based tracking.
 */
@RestController
@RequestMapping("/api/requests")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    public PurchaseRequestController(PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> createDraft(
            @Valid @RequestBody CreatePurchaseRequest request,
            Authentication authentication) {
        PurchaseRequestResponse response = purchaseRequestService.createDraft(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePurchaseRequest request,
            Authentication authentication) {
        PurchaseRequestResponse response = purchaseRequestService.updateDraft(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> submitRequest(
            @PathVariable Long id,
            Authentication authentication) {
        PurchaseRequestResponse response = purchaseRequestService.submitPurchaseRequest(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> cancelRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        PurchaseRequestResponse response = purchaseRequestService.cancelPurchaseRequest(id, reason, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDraft(
            @PathVariable Long id,
            Authentication authentication) {
        purchaseRequestService.deleteDraft(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> getRequestById(
            @PathVariable Long id,
            Authentication authentication) {
        PurchaseRequestResponse response = purchaseRequestService.getPurchaseRequestById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{requestNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseRequestResponse> getRequestByNumber(
            @PathVariable String requestNumber,
            Authentication authentication) {
        PurchaseRequestResponse response = purchaseRequestService.getPurchaseRequestByNumber(requestNumber, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PurchaseRequestResponse>> getMyRequests(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PurchaseRequestResponse> response = purchaseRequestService.getMyRequests(authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'PROCUREMENT')")
    public ResponseEntity<Page<PurchaseRequestResponse>> getDepartmentRequests(
            @PathVariable Long departmentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PurchaseRequestResponse> response = purchaseRequestService.getDepartmentRequests(departmentId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'PROCUREMENT')")
    public ResponseEntity<Page<PurchaseRequestResponse>> filterRequests(
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) RequestStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PurchaseRequestResponse> response = purchaseRequestService.filterRequests(
                requesterId, departmentId, status, pageable);
        return ResponseEntity.ok(response);
    }
}
