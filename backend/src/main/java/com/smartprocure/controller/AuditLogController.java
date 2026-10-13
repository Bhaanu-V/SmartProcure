package com.smartprocure.controller;

import com.smartprocure.dto.audit.AuditLogResponse;
import com.smartprocure.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller providing compliance officers and administrators with immutable audit trails.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAllAuditLogs(
            @PageableDefault(size = 30, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogResponse> response = auditService.getAllAuditLogs(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/entity/{entityName}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'PROCUREMENT', 'MANAGER')")
    public ResponseEntity<List<AuditLogResponse>> getEntityAuditHistory(
            @PathVariable String entityName,
            @PathVariable Long entityId) {
        List<AuditLogResponse> response = auditService.getEntityAuditHistory(entityName, entityId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getUserAuditHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 30, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogResponse> response = auditService.getUserAuditHistory(userId, pageable);
        return ResponseEntity.ok(response);
    }
}
