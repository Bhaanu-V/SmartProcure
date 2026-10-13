package com.smartprocure.service;

import com.smartprocure.audit.Auditable;
import com.smartprocure.dto.approval.ApprovalDecisionRequest;
import com.smartprocure.dto.pr.PurchaseRequestResponse;
import com.smartprocure.entity.*;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.NotificationRepository;
import com.smartprocure.repository.PurchaseRequestRepository;
import com.smartprocure.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service managing Level 2 (Finance) review, budget reservation, approval, and queue.
 */
@Service
public class FinanceApprovalService {

    private static final Logger log = LoggerFactory.getLogger(FinanceApprovalService.class);

    private final PurchaseRequestRepository requestRepository;
    private final ApprovalWorkflowService workflowService;
    private final BudgetService budgetService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public FinanceApprovalService(PurchaseRequestRepository requestRepository,
                                  ApprovalWorkflowService workflowService,
                                  BudgetService budgetService,
                                  UserRepository userRepository,
                                  NotificationRepository notificationRepository) {
        this.requestRepository = requestRepository;
        this.workflowService = workflowService;
        this.budgetService = budgetService;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Executes finance approval or rejection decision with budget reservation.
     */
    @Auditable(action = "PR_FINANCE_DECISION", entityName = "PurchaseRequest")
    @Transactional
    public PurchaseRequestResponse processDecision(Long requestId, ApprovalDecisionRequest decision, String actorPrincipal) {
        PurchaseRequest pr = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", requestId));

        User actor = workflowService.findUserByPrincipal(actorPrincipal);

        workflowService.validateFinanceDecision(pr, actor, decision);

        int fiscalYear = pr.getCreatedAt() != null ? pr.getCreatedAt().getYear() : LocalDate.now().getYear();

        if (decision.getAction() == ApprovalAction.APPROVED) {
            if (pr.getDepartment() == null) {
                throw new BadRequestException("Purchase request must be associated with a valid department");
            }

            try {
                budgetService.reserveBudget(pr.getDepartment().getId(), fiscalYear, pr.getTotalAmount());
            } catch (ResourceNotFoundException ex) {
                throw new BadRequestException(String.format(
                        "No budget allocated for department '%s' in FY %d. Please allocate budget before finance approval.",
                        pr.getDepartment().getName(), fiscalYear));
            }

            pr.setStatus(RequestStatus.APPROVED);
            workflowService.recordDecision(pr, actor, ApprovalAction.APPROVED, ApprovalStage.FINANCE, decision.getComments());

            // Notify requester
            notifyRequester(pr, "Requisition " + pr.getRequestNumber() + " Approved by Finance",
                    String.format("Finance officer %s approved your requisition '%s' ($%s). Budget reserved.",
                            actor.getFullName(), pr.getTitle(), pr.getTotalAmount()),
                    NotificationType.APPROVED);

            // Notify Procurement team
            notifyProcurementTeam(pr, actor);

            log.info("Finance officer '{}' approved PR '{}' with budget reservation. Status is now APPROVED",
                    actor.getUsername(), pr.getRequestNumber());
        } else {
            pr.setStatus(RequestStatus.REJECTED);
            pr.setRejectionReason(decision.getComments().trim());
            workflowService.recordDecision(pr, actor, ApprovalAction.REJECTED, ApprovalStage.FINANCE, decision.getComments());

            // Notify requester of rejection
            notifyRequester(pr, "Requisition " + pr.getRequestNumber() + " Rejected by Finance",
                    String.format("Finance officer %s rejected your requisition '%s'. Reason: %s",
                            actor.getFullName(), pr.getTitle(), decision.getComments().trim()),
                    NotificationType.REJECTED);

            log.info("Finance officer '{}' rejected PR '{}'. Reason: {}",
                    actor.getUsername(), pr.getRequestNumber(), decision.getComments());
        }

        PurchaseRequest saved = requestRepository.save(pr);
        return PurchaseRequestResponse.fromEntity(saved);
    }

    /**
     * Retrieves the pending finance queue (all requests awaiting finance review).
     */
    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> getPendingQueue(Pageable pageable) {
        return requestRepository.findByStatus(RequestStatus.PENDING_FINANCE_APPROVAL, pageable)
                .map(PurchaseRequestResponse::fromEntity);
    }

    private void notifyRequester(PurchaseRequest pr, String title, String message, NotificationType type) {
        if (pr.getRequester() != null) {
            Notification notification = new Notification(
                    pr.getRequester(),
                    title,
                    message,
                    type,
                    "PURCHASE_REQUEST",
                    pr.getId()
            );
            notificationRepository.save(notification);
        }
    }

    private void notifyProcurementTeam(PurchaseRequest pr, User financeOfficer) {
        List<User> procurementUsers = userRepository.findByRole(Role.PROCUREMENT);
        for (User proc : procurementUsers) {
            Notification notification = new Notification(
                    proc,
                    "Requisition Ready for PO: " + pr.getRequestNumber(),
                    String.format("Finance approved requisition '%s' ($%s) for department %s. Ready to generate Purchase Order.",
                            pr.getTitle(), pr.getTotalAmount(),
                            pr.getDepartment() != null ? pr.getDepartment().getName() : "N/A"),
                    NotificationType.INFO,
                    "PURCHASE_REQUEST",
                    pr.getId()
            );
            notificationRepository.save(notification);
        }
    }
}
