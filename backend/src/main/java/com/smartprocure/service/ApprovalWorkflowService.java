package com.smartprocure.service;

import com.smartprocure.dto.approval.ApprovalDecisionRequest;
import com.smartprocure.dto.approval.ApprovalHistoryResponse;
import com.smartprocure.entity.*;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.ApprovalHistoryRepository;
import com.smartprocure.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing state machine validations, self-approval prevention,
 * and immutable decision history tracking across procurement approval stages.
 */
@Service
public class ApprovalWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalWorkflowService.class);

    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final UserRepository userRepository;

    public ApprovalWorkflowService(ApprovalHistoryRepository approvalHistoryRepository,
                                  UserRepository userRepository) {
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Validates that the manager approval stage is eligible and actor has appropriate permissions.
     * Prevents self-approval and verifies required rejection comments.
     */
    public void validateManagerDecision(PurchaseRequest pr, User actor, ApprovalDecisionRequest decision) {
        if (pr.getStatus() != RequestStatus.PENDING_MANAGER_APPROVAL) {
            throw new BadRequestException(
                    "Request " + pr.getRequestNumber() + " is not pending manager approval. Current status: " + pr.getStatus());
        }

        if (pr.getRequester().getId().equals(actor.getId())) {
            log.warn("Security violation: User '{}' attempted to self-approve request '{}'",
                    actor.getUsername(), pr.getRequestNumber());
            throw new AccessDeniedException("Requesters are strictly prohibited from approving their own purchase requisitions");
        }

        boolean isAdmin = actor.getRole() == Role.ADMIN;
        boolean isDeptManager = pr.getDepartment() != null &&
                pr.getDepartment().getManager() != null &&
                pr.getDepartment().getManager().getId().equals(actor.getId());
        boolean isGeneralManager = actor.getRole() == Role.MANAGER;

        if (!isAdmin && !isDeptManager && !isGeneralManager) {
            throw new AccessDeniedException("Only designated department managers or administrators can review this stage");
        }

        if (decision.getAction() == ApprovalAction.REJECTED) {
            if (decision.getComments() == null || decision.getComments().trim().isEmpty()) {
                throw new BadRequestException("Rejection comments are mandatory when rejecting a purchase requisition");
            }
        }
    }

    /**
     * Validates that the finance approval stage is eligible and actor has finance authorization.
     * Prevents self-approval and verifies required rejection comments.
     */
    public void validateFinanceDecision(PurchaseRequest pr, User actor, ApprovalDecisionRequest decision) {
        if (pr.getStatus() != RequestStatus.PENDING_FINANCE_APPROVAL) {
            throw new BadRequestException(
                    "Request " + pr.getRequestNumber() + " is not pending finance approval. Current status: " + pr.getStatus());
        }

        if (pr.getRequester().getId().equals(actor.getId())) {
            log.warn("Security violation: User '{}' attempted to self-approve request '{}' at finance stage",
                    actor.getUsername(), pr.getRequestNumber());
            throw new AccessDeniedException("Requesters are strictly prohibited from approving their own purchase requisitions");
        }

        boolean isAdmin = actor.getRole() == Role.ADMIN;
        boolean isFinance = actor.getRole() == Role.FINANCE;

        if (!isAdmin && !isFinance) {
            throw new AccessDeniedException("Only finance officers or administrators can review this stage");
        }

        if (decision.getAction() == ApprovalAction.REJECTED) {
            if (decision.getComments() == null || decision.getComments().trim().isEmpty()) {
                throw new BadRequestException("Rejection comments are mandatory when rejecting a purchase requisition");
            }
        }
    }

    /**
     * Records an immutable approval history entry.
     */
    @Transactional
    public ApprovalHistory recordDecision(PurchaseRequest pr, User actor, ApprovalAction action, ApprovalStage stage, String comments) {
        ApprovalHistory history = new ApprovalHistory(
                pr,
                actor,
                action,
                stage,
                comments != null ? comments.trim() : null
        );
        ApprovalHistory saved = approvalHistoryRepository.save(history);
        log.info("Recorded approval history: Request '{}', Stage: {}, Action: {}, Actor: '{}'",
                pr.getRequestNumber(), stage, action, actor.getUsername());
        return saved;
    }

    /**
     * Retrieves the chronological decision history for a given purchase requisition.
     */
    @Transactional(readOnly = true)
    public List<ApprovalHistoryResponse> getRequestApprovalHistory(Long requestId) {
        return approvalHistoryRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(requestId)
                .stream()
                .map(ApprovalHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Resolves user by either username or email.
     */
    public User findUserByPrincipal(String principal) {
        return userRepository.findByEmail(principal)
                .or(() -> userRepository.findByUsername(principal))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username or email", principal));
    }
}
