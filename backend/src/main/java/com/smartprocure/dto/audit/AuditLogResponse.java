package com.smartprocure.dto.audit;

import com.smartprocure.entity.AuditLog;

import java.time.OffsetDateTime;

public class AuditLogResponse {

    private Long id;
    private String entityName;
    private Long entityId;
    private String action;
    private Long actorId;
    private String actorUsername;
    private String details;
    private String ipAddress;
    private OffsetDateTime timestamp;

    public AuditLogResponse() {
    }

    public AuditLogResponse(Long id,
                            String entityName,
                            Long entityId,
                            String action,
                            Long actorId,
                            String actorUsername,
                            String details,
                            String ipAddress,
                            OffsetDateTime timestamp) {
        this.id = id;
        this.entityName = entityName;
        this.entityId = entityId;
        this.action = action;
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    public static AuditLogResponse fromEntity(AuditLog log) {
        if (log == null) {
            return null;
        }
        Long actorId = log.getActor() != null ? log.getActor().getId() : null;
        return new AuditLogResponse(
                log.getId(),
                log.getEntityName(),
                log.getEntityId(),
                log.getAction(),
                actorId,
                log.getActorUsername(),
                log.getDetails(),
                log.getIpAddress(),
                log.getTimestamp()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
