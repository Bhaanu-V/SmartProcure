package com.smartprocure.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking business operations that require persistent compliance auditing.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Action name (e.g. "PR_CREATED", "PR_SUBMITTED", "BUDGET_RESERVED", "PO_ISSUED").
     */
    String action();

    /**
     * Target domain entity name (e.g. "PurchaseRequest", "PurchaseOrder", "DepartmentBudget").
     */
    String entityName() default "";
}
