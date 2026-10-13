package com.smartprocure.controller;

import com.smartprocure.dto.approval.ApprovalDecisionRequest;
import com.smartprocure.dto.approval.ApprovalHistoryResponse;
import com.smartprocure.dto.pr.PurchaseRequestResponse;
import com.smartprocure.service.ApprovalWorkflowService;
import com.smartprocure.service.FinanceApprovalService;
import com.smartprocure.service.ManagerApprovalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for multi-tier procurement approvals, queues, and decision history.
 */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ManagerApprovalService managerApprovalService;
    private final FinanceApprovalService financeApprovalService;
    private final ApprovalWorkflowService workflowService;

    public ApprovalController(ManagerApprovalService managerApprovalService,
                              FinanceApprovalService financeApprovalService,
                              ApprovalWorkflowService workflowService) {
        this.managerApprovalService = managerApprovalService;
        this.financeApprovalService = financeApprovalService;
        this.workflowService = workflowService;
    }

    /**
     * Submits a Level 1 (Manager) approval or rejection decision.
     */
    @PostMapping("/{requestId}/manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PurchaseRequestResponse> reviewByManager(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalDecisionRequest decision,
            Authentication authentication) {
        PurchaseRequestResponse response = managerApprovalService.processDecision(
                requestId, decision, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Submits a Level 2 (Finance) approval or rejection decision with budget reservation.
     */
    @PostMapping("/{requestId}/finance")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<PurchaseRequestResponse> reviewByFinance(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalDecisionRequest decision,
            Authentication authentication) {
        PurchaseRequestResponse response = financeApprovalService.processDecision(
                requestId, decision, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the pending manager approval queue for the authenticated user.
     */
    @GetMapping("/queue/manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<PurchaseRequestResponse>> getManagerQueue(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PurchaseRequestResponse> queue = managerApprovalService.getPendingQueue(
                authentication.getName(), pageable);
        return ResponseEntity.ok(queue);
    }

    /**
     * Retrieves the pending finance approval queue.
     */
    @GetMapping("/queue/finance")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public ResponseEntity<Page<PurchaseRequestResponse>> getFinanceQueue(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PurchaseRequestResponse> queue = financeApprovalService.getPendingQueue(pageable);
        return ResponseEntity.ok(queue);
    }

    /**
     * Retrieves the complete chronological approval history and audit trail for a requisition.
     */
    @GetMapping("/history/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApprovalHistoryResponse>> getApprovalHistory(
            @PathVariable Long requestId) {
        List<ApprovalHistoryResponse> history = workflowService.getRequestApprovalHistory(requestId);
        return ResponseEntity.ok(history);
    }
}
