package com.smartprocure.entity;

/**
 * Status lifecycle transitions for Purchase Requests.
 */
public enum RequestStatus {
    DRAFT,
    SUBMITTED,
    PENDING_MANAGER_APPROVAL,
    PENDING_FINANCE_APPROVAL,
    APPROVED,
    REJECTED,
    PO_CREATED,
    CANCELLED
}
