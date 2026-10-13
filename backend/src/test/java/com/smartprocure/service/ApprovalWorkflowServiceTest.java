package com.smartprocure.service;

import com.smartprocure.dto.approval.ApprovalDecisionRequest;
import com.smartprocure.dto.pr.PurchaseRequestResponse;
import com.smartprocure.entity.*;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.repository.DepartmentRepository;
import com.smartprocure.repository.NotificationRepository;
import com.smartprocure.repository.PurchaseRequestRepository;
import com.smartprocure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowServiceTest {

    @Mock
    private com.smartprocure.repository.ApprovalHistoryRepository approvalHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PurchaseRequestRepository requestRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private ApprovalWorkflowService workflowService;

    private User requester;
    private User manager;
    private User financeOfficer;
    private Department department;
    private PurchaseRequest purchaseRequest;

    @BeforeEach
    void setUp() {
        requester = new User("requester", "requester@smartprocure.io", "pass", "Requester User", Role.EMPLOYEE);
        requester.setId(10L);

        manager = new User("dept_manager", "manager@smartprocure.io", "pass", "Dept Manager", Role.MANAGER);
        manager.setId(20L);

        financeOfficer = new User("finance_user", "finance@smartprocure.io", "pass", "Finance Officer", Role.FINANCE);
        financeOfficer.setId(30L);

        department = new Department("ENG", "Engineering", "Software Dev");
        department.setId(100L);
        department.setManager(manager);

        purchaseRequest = new PurchaseRequest("PR-20261014-001", "MacBook Pro", requester, department);
        purchaseRequest.setId(500L);
        purchaseRequest.setStatus(RequestStatus.PENDING_MANAGER_APPROVAL);
        purchaseRequest.setTotalAmount(new BigDecimal("2500.00"));
    }

    @Test
    @DisplayName("Self-approval check: Requester cannot approve their own purchase requisition at manager stage")
    void testSelfApprovalPrevented_ManagerStage() {
        ApprovalDecisionRequest decision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Self approval attempt");

        assertThrows(AccessDeniedException.class, () ->
                workflowService.validateManagerDecision(purchaseRequest, requester, decision));
    }

    @Test
    @DisplayName("Self-approval check: Requester cannot approve their own purchase requisition at finance stage")
    void testSelfApprovalPrevented_FinanceStage() {
        purchaseRequest.setStatus(RequestStatus.PENDING_FINANCE_APPROVAL);
        ApprovalDecisionRequest decision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Self approval attempt");

        assertThrows(AccessDeniedException.class, () ->
                workflowService.validateFinanceDecision(purchaseRequest, requester, decision));
    }

    @Test
    @DisplayName("Stage validation: Rejection requires explanatory comments")
    void testRejectionRequiresComments() {
        ApprovalDecisionRequest decisionWithoutComments = new ApprovalDecisionRequest(ApprovalAction.REJECTED, null);

        assertThrows(BadRequestException.class, () ->
                workflowService.validateManagerDecision(purchaseRequest, manager, decisionWithoutComments));
    }

    @Test
    @DisplayName("Stage validation: Manager decision fails if PR is not in PENDING_MANAGER_APPROVAL")
    void testManagerDecisionWrongStatus() {
        purchaseRequest.setStatus(RequestStatus.DRAFT);
        ApprovalDecisionRequest decision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Looks good");

        assertThrows(BadRequestException.class, () ->
                workflowService.validateManagerDecision(purchaseRequest, manager, decision));
    }

    @Test
    @DisplayName("Finance stage validation: Non-finance user cannot review finance stage")
    void testFinanceDecisionRequiresFinanceRole() {
        purchaseRequest.setStatus(RequestStatus.PENDING_FINANCE_APPROVAL);
        ApprovalDecisionRequest decision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Finance approval");

        assertThrows(AccessDeniedException.class, () ->
                workflowService.validateFinanceDecision(purchaseRequest, manager, decision));
    }

    @Test
    @DisplayName("Manager approval service: Successfully approves and progresses to PENDING_FINANCE_APPROVAL")
    void testManagerApprovalService_Approved() {
        ManagerApprovalService managerService = new ManagerApprovalService(
                requestRepository, workflowService, userRepository, notificationRepository, departmentRepository);

        when(requestRepository.findById(500L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findByEmail("manager@smartprocure.io")).thenReturn(Optional.of(manager));
        when(requestRepository.save(any(PurchaseRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ApprovalDecisionRequest decision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Approved for team");
        PurchaseRequestResponse response = managerService.processDecision(500L, decision, "manager@smartprocure.io");

        assertNotNull(response);
        assertEquals(RequestStatus.PENDING_FINANCE_APPROVAL, response.getStatus());
        verify(approvalHistoryRepository, times(1)).save(any(ApprovalHistory.class));
    }

    @Test
    @DisplayName("Finance approval service: Successfully reserves budget and transitions to APPROVED")
    void testFinanceApprovalService_Approved() {
        purchaseRequest.setStatus(RequestStatus.PENDING_FINANCE_APPROVAL);

        FinanceApprovalService financeService = new FinanceApprovalService(
                requestRepository, workflowService, budgetService, userRepository, notificationRepository);

        when(requestRepository.findById(500L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findByEmail("finance@smartprocure.io")).thenReturn(Optional.of(financeOfficer));
        when(requestRepository.save(any(PurchaseRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ApprovalDecisionRequest decision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Budget verified");
        PurchaseRequestResponse response = financeService.processDecision(500L, decision, "finance@smartprocure.io");

        assertNotNull(response);
        assertEquals(RequestStatus.APPROVED, response.getStatus());
        verify(budgetService, times(1)).reserveBudget(eq(100L), anyInt(), eq(new BigDecimal("2500.00")));
        verify(approvalHistoryRepository, times(1)).save(any(ApprovalHistory.class));
    }
}
