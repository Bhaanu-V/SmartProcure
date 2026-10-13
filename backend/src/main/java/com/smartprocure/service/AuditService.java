package com.smartprocure.service;

import com.smartprocure.dto.audit.AuditLogResponse;
import com.smartprocure.entity.AuditLog;
import com.smartprocure.entity.User;
import com.smartprocure.repository.AuditLogRepository;
import com.smartprocure.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing immutable audit logs for regulatory compliance and lifecycle tracing.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Persists an audit entry in a new transaction so audit logs are never rolled back
     * even if subsequent operations fail.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogResponse recordAudit(String entityName,
                                        Long entityId,
                                        String action,
                                        String username,
                                        String details,
                                        String ipAddress) {
        User actor = null;
        if (username != null && !username.trim().isEmpty()) {
            actor = userRepository.findByEmail(username).orElse(null);
            if (actor == null) {
                actor = userRepository.findByUsername(username).orElse(null);
            }
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId != null ? entityId : 0L);
        auditLog.setAction(action);
        auditLog.setActor(actor);
        auditLog.setActorUsername(actor != null ? actor.getUsername() : (username != null ? username : "SYSTEM"));
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("AUDIT: [{}] {} on {} (ID: {}) by {}", action, details, entityName, entityId, auditLog.getActorUsername());
        return AuditLogResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityAuditHistory(String entityName, Long entityId) {
        return auditLogRepository.findByEntityNameAndEntityIdOrderByTimestampDesc(entityName, entityId).stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getUserAuditHistory(Long userId, Pageable pageable) {
        return auditLogRepository.findByActorIdOrderByTimestampDesc(userId, pageable)
                .map(AuditLogResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(AuditLogResponse::fromEntity);
    }
}
