package com.smartprocure.controller;

import com.smartprocure.dto.po.GeneratePurchaseOrderRequest;
import com.smartprocure.dto.po.PurchaseOrderResponse;
import com.smartprocure.dto.po.UpdatePOStatusRequest;
import com.smartprocure.entity.POStatus;
import com.smartprocure.service.PdfGenerationService;
import com.smartprocure.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for issuing Purchase Orders, managing lifecycle statuses,
 * exporting certified PDF documents, and querying order records with role-based access control.
 */
@RestController
@RequestMapping("/api/orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final PdfGenerationService pdfGenerationService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService,
                                   PdfGenerationService pdfGenerationService) {
        this.purchaseOrderService = purchaseOrderService;
        this.pdfGenerationService = pdfGenerationService;
    }

    /**
     * Issues an official Purchase Order from an approved Purchase Request.
     */
    @PostMapping("/from-request/{requestId}")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> generateFromRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody GeneratePurchaseOrderRequest request,
            Authentication authentication) {
        PurchaseOrderResponse response = purchaseOrderService.generateFromApprovedRequest(
                requestId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a purchase order by internal ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseOrderResponse> getOrderById(
            @PathVariable Long id,
            Authentication authentication) {
        PurchaseOrderResponse response = purchaseOrderService.getPurchaseOrderById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a purchase order by unique PO number (e.g. PO-20261014-1234).
     */
    @GetMapping("/number/{poNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PurchaseOrderResponse> getOrderByNumber(
            @PathVariable String poNumber,
            Authentication authentication) {
        PurchaseOrderResponse response = purchaseOrderService.getPurchaseOrderByNumber(poNumber, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Queries and filters purchase orders across suppliers, status, and search keywords.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'FINANCE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Page<PurchaseOrderResponse>> filterOrders(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) POStatus status,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PurchaseOrderResponse> orders = purchaseOrderService.filterOrders(
                supplierId, status, query, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Updates the status lifecycle of an existing purchase order.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePOStatusRequest request,
            Authentication authentication) {
        PurchaseOrderResponse response = purchaseOrderService.updateOrderStatus(
                id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Generates and downloads a digitally formatted PDF document for an authorized Purchase Order.
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadOrderPdf(
            @PathVariable Long id,
            Authentication authentication) {
        com.smartprocure.entity.PurchaseOrder po = purchaseOrderService.getEntityForExport(
                id, authentication.getName());
        byte[] pdfBytes = pdfGenerationService.generatePurchaseOrderPdf(po);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", po.getPoNumber() + ".pdf");
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
