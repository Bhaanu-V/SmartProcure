package com.smartprocure.service;

import com.smartprocure.dto.pr.CreateLineItemRequest;
import com.smartprocure.dto.pr.CreatePurchaseRequest;
import com.smartprocure.dto.pr.PurchaseRequestResponse;
import com.smartprocure.dto.pr.UpdatePurchaseRequest;
import com.smartprocure.entity.*;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.repository.*;
import com.smartprocure.validator.PurchaseRequestCalculationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestServiceTest {

    @Mock
    private PurchaseRequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CatalogItemRepository catalogItemRepository;

    @Mock
    private PurchaseRequestCalculationValidator calculationValidator;

    @Mock
    private BudgetService budgetService;

    @Mock
    private ApprovalHistoryRepository approvalHistoryRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private PurchaseRequestService purchaseRequestService;

    private User requester;
    private User manager;
    private Department department;
    private PurchaseRequest purchaseRequest;

    @BeforeEach
    void setUp() {
        requester = new User("alice", "alice@smartprocure.io", "hash", "Alice Requester", Role.EMPLOYEE);
        requester.setId(1L);

        manager = new User("bob", "bob@smartprocure.io", "hash", "Bob Manager", Role.MANAGER);
        manager.setId(2L);

        department = new Department("IT", "Information Tech", "IT Infrastructure");
        department.setId(10L);
        department.setManager(manager);

        purchaseRequest = new PurchaseRequest("PR-20261014-1001", "Developer Hardware", requester, department);
        purchaseRequest.setId(100L);
        purchaseRequest.setStatus(RequestStatus.DRAFT);
        purchaseRequest.setTotalAmount(new BigDecimal("1500.00"));
        purchaseRequest.setLineItems(new ArrayList<>());
    }

    @Test
    @DisplayName("Create draft successfully calculates totals and saves PR in DRAFT status")
    void testCreateDraft_Success() {
        CreateLineItemRequest item1 = new CreateLineItemRequest("Laptop Stand", new BigDecimal("2.00"), new BigDecimal("50.00"));
        CreatePurchaseRequest request = new CreatePurchaseRequest("Office Gear", 10L, Collections.singletonList(item1));

        when(userRepository.findByEmail("alice@smartprocure.io")).thenReturn(Optional.of(requester));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(calculationValidator.validateAndCalculateLineTotal(any())).thenReturn(new BigDecimal("100.00"));
        when(calculationValidator.calculateGrandTotal(any())).thenReturn(new BigDecimal("100.00"));
        when(requestRepository.save(any(PurchaseRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequestResponse response = purchaseRequestService.createDraft(request, "alice@smartprocure.io");

        assertNotNull(response);
        assertEquals(RequestStatus.DRAFT, response.getStatus());
        assertEquals("Office Gear", response.getTitle());
        assertEquals(0, response.getTotalAmount().compareTo(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("Update draft succeeds when owner edits draft")
    void testUpdateDraft_Success() {
        UpdatePurchaseRequest update = new UpdatePurchaseRequest();
        update.setTitle("Updated Title");

        when(requestRepository.findById(100L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findByEmail("alice@smartprocure.io")).thenReturn(Optional.of(requester));
        when(requestRepository.save(any(PurchaseRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequestResponse response = purchaseRequestService.updateDraft(100L, update, "alice@smartprocure.io");

        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
    }

    @Test
    @DisplayName("Update draft throws AccessDeniedException when another user attempts to edit")
    void testUpdateDraft_UnauthorizedUser() {
        User otherUser = new User("charlie", "charlie@smartprocure.io", "hash", "Charlie", Role.EMPLOYEE);
        otherUser.setId(3L);

        UpdatePurchaseRequest update = new UpdatePurchaseRequest();
        update.setTitle("Sneaky Edit");

        when(requestRepository.findById(100L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findByEmail("charlie@smartprocure.io")).thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class, () ->
                purchaseRequestService.updateDraft(100L, update, "charlie@smartprocure.io"));
    }

    @Test
    @DisplayName("Submit purchase request transitions status to PENDING_MANAGER_APPROVAL and creates notification")
    void testSubmitPurchaseRequest_Success() {
        PurchaseRequestItem item = new PurchaseRequestItem("Monitor", new BigDecimal("1.00"), new BigDecimal("300.00"));
        purchaseRequest.addLineItem(item);

        when(requestRepository.findById(100L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findByEmail("alice@smartprocure.io")).thenReturn(Optional.of(requester));
        when(requestRepository.save(any(PurchaseRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequestResponse response = purchaseRequestService.submitPurchaseRequest(100L, "alice@smartprocure.io");

        assertNotNull(response);
        assertEquals(RequestStatus.PENDING_MANAGER_APPROVAL, response.getStatus());
        verify(approvalHistoryRepository, times(1)).save(any(ApprovalHistory.class));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Submit purchase request throws BadRequestException when items list is empty")
    void testSubmitPurchaseRequest_EmptyItems() {
        when(requestRepository.findById(100L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findByEmail("alice@smartprocure.io")).thenReturn(Optional.of(requester));

        assertThrows(BadRequestException.class, () ->
                purchaseRequestService.submitPurchaseRequest(100L, "alice@smartprocure.io"));
    }

    @Test
    @DisplayName("Cancel purchase request transitions status to CANCELLED")
    void testCancelPurchaseRequest_Success() {
        when(requestRepository.findById(100L)).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findByEmail("alice@smartprocure.io")).thenReturn(Optional.of(requester));
        when(requestRepository.save(any(PurchaseRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequestResponse response = purchaseRequestService.cancelPurchaseRequest(100L, "Project postponed", "alice@smartprocure.io");

        assertNotNull(response);
        assertEquals(RequestStatus.CANCELLED, response.getStatus());
        verify(approvalHistoryRepository, times(1)).save(any(ApprovalHistory.class));
    }
}
