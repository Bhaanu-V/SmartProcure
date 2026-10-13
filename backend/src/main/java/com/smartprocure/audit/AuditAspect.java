package com.smartprocure.audit;

import com.smartprocure.dto.budget.BudgetResponse;
import com.smartprocure.dto.pr.PurchaseRequestResponse;
import com.smartprocure.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * AOP aspect intercepting methods annotated with @Auditable to capture compliance audit entries.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditMethodExecution(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            String username = resolveCurrentUsername();
            String ipAddress = resolveClientIpAddress();
            String entityName = auditable.entityName().isEmpty()
                    ? resolveEntityName(joinPoint, result)
                    : auditable.entityName();

            Long entityId = resolveEntityId(joinPoint, result);
            String details = String.format("Action '%s' performed via %s",
                    auditable.action(), joinPoint.getSignature().getName());

            auditService.recordAudit(entityName, entityId, auditable.action(), username, details, ipAddress);
        } catch (Exception ex) {
            log.error("Failed to capture AOP audit log for action '{}': {}", auditable.action(), ex.getMessage());
        }
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private String resolveClientIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    private String resolveEntityName(JoinPoint joinPoint, Object result) {
        if (result instanceof PurchaseRequestResponse) {
            return "PurchaseRequest";
        } else if (result instanceof BudgetResponse) {
            return "DepartmentBudget";
        }
        return joinPoint.getTarget().getClass().getSimpleName().replace("Service", "");
    }

    private Long resolveEntityId(JoinPoint joinPoint, Object result) {
        if (result != null) {
            try {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                if (id instanceof Long) {
                    return (Long) id;
                }
            } catch (Exception ignored) {
            }
        }

        // Fallback: inspect method arguments for a Long id
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return 0L;
    }
}
