package com.smartprocure.entity;

/**
 * Status lifecycle transitions for Purchase Orders.
 */
public enum POStatus {
    DRAFT,
    ISSUED,
    PARTIALLY_FULFILLED,
    FULFILLED,
    CANCELLED
}
