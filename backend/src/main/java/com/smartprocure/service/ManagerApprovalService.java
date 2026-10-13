package com.smartprocure.service;

import com.smartprocure.audit.Auditable;
import com.smartprocure.dto.approval.ApprovalDecisionRequest;
import com.smartprocure.dto.pr.PurchaseRequestResponse;
import com.smartprocure.entity.*;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.DepartmentRepository;
import com.smartprocure.repository.NotificationRepository;
import com.smartprocure.repository.PurchaseRequestRepository;
import com.smartprocure.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing Level 1 (Manager) review, approval, rejection, and queues.
 */
@Service
public class ManagerApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ManagerApprovalService.class);

    private final PurchaseRequestRepository requestRepository;
    private final ApprovalWorkflowService workflowService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final DepartmentRepository departmentRepository;

    public ManagerApprovalService(PurchaseRequestRepository requestRepository,
                                  ApprovalWorkflowService workflowService,
                                  UserRepository userRepository,
                                  NotificationRepository notificationRepository,
                                  DepartmentRepository departmentRepository) {
        this.requestRepository = requestRepository;
        this.workflowService = workflowService;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Executes manager approval or rejection decision.
     */
    @Auditable(action = "PR_MANAGER_DECISION", entityName = "PurchaseRequest")
    @Transactional
    public PurchaseRequestResponse processDecision(Long requestId, ApprovalDecisionRequest decision, String actorPrincipal) {
        PurchaseRequest pr = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", requestId));

        User actor = workflowService.findUserByPrincipal(actorPrincipal);

        workflowService.validateManagerDecision(pr, actor, decision);

        if (decision.getAction() == ApprovalAction.APPROVED) {
            pr.setStatus(RequestStatus.PENDING_FINANCE_APPROVAL);
            workflowService.recordDecision(pr, actor, ApprovalAction.APPROVED, ApprovalStage.MANAGER, decision.getComments());

            // Notify requester
            notifyRequester(pr, "Requisition " + pr.getRequestNumber() + " Approved by Manager",
                    String.format("Manager %s approved your requisition '%s'. It has progressed to Finance review.",
                            actor.getFullName(), pr.getTitle()),
                    NotificationType.APPROVED);

            // Notify Finance officers
            notifyFinanceTeam(pr, actor);

            log.info("Manager '{}' approved PR '{}'. Status progressed to PENDING_FINANCE_APPROVAL",
                    actor.getUsername(), pr.getRequestNumber());
        } else {
            pr.setStatus(RequestStatus.REJECTED);
            pr.setRejectionReason(decision.getComments().trim());
            workflowService.recordDecision(pr, actor, ApprovalAction.REJECTED, ApprovalStage.MANAGER, decision.getComments());

            // Notify requester of rejection
            notifyRequester(pr, "Requisition " + pr.getRequestNumber() + " Rejected by Manager",
                    String.format("Manager %s rejected your requisition '%s'. Reason: %s",
                            actor.getFullName(), pr.getTitle(), decision.getComments().trim()),
                    NotificationType.REJECTED);

            log.info("Manager '{}' rejected PR '{}'. Reason: {}",
                    actor.getUsername(), pr.getRequestNumber(), decision.getComments());
        }

        PurchaseRequest saved = requestRepository.save(pr);
        return PurchaseRequestResponse.fromEntity(saved);
    }

    /**
     * Retrieves the pending manager approval queue for the authenticated manager.
     */
    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> getPendingQueue(String managerPrincipal, Pageable pageable) {
        User manager = workflowService.findUserByPrincipal(managerPrincipal);

        if (manager.getRole() == Role.ADMIN) {
            return requestRepository.findByStatus(RequestStatus.PENDING_MANAGER_APPROVAL, pageable)
                    .map(PurchaseRequestResponse::fromEntity);
        }

        if (manager.getDepartmentEntity() != null) {
            return requestRepository.findByDepartmentIdAndStatus(
                    manager.getDepartmentEntity().getId(), RequestStatus.PENDING_MANAGER_APPROVAL, pageable)
                    .map(PurchaseRequestResponse::fromEntity);
        }

        List<Department> managedDepartments = departmentRepository.findByManagerId(manager.getId());
        if (!managedDepartments.isEmpty()) {
            Long deptId = managedDepartments.get(0).getId();
            return requestRepository.findByDepartmentIdAndStatus(
                    deptId, RequestStatus.PENDING_MANAGER_APPROVAL, pageable)
                    .map(PurchaseRequestResponse::fromEntity);
        }

        return requestRepository.findByStatus(RequestStatus.PENDING_MANAGER_APPROVAL, pageable)
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

    private void notifyFinanceTeam(PurchaseRequest pr, User manager) {
        List<User> financeUsers = userRepository.findByRole(Role.FINANCE);
        for (User finance : financeUsers) {
            Notification notification = new Notification(
                    finance,
                    "Requisition Pending Finance Approval: " + pr.getRequestNumber(),
                    String.format("Manager %s approved '%s' ($%s) for department %s. Awaiting budget verification.",
                            manager.getFullName(), pr.getTitle(), pr.getTotalAmount(),
                            pr.getDepartment() != null ? pr.getDepartment().getName() : "N/A"),
                    NotificationType.APPROVAL_REQUEST,
                    "PURCHASE_REQUEST",
                    pr.getId()
            );
            notificationRepository.save(notification);
        }
    }
}
