package com.smartprocure.service;

import com.smartprocure.dto.po.GeneratePurchaseOrderRequest;
import com.smartprocure.dto.po.PurchaseOrderResponse;
import com.smartprocure.dto.po.UpdatePOStatusRequest;
import com.smartprocure.entity.*;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository orderRepository;

    @Mock
    private PurchaseRequestRepository requestRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BudgetService budgetService;

    @Mock
    private ApprovalHistoryRepository approvalHistoryRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private User requester;
    private User procurementUser;
    private Department department;
    private Supplier supplier;
    private PurchaseRequest approvedRequest;

    @BeforeEach
    void setUp() {
        requester = new User("po_requester", "requester@smartprocure.io", "pass", "Requester", Role.EMPLOYEE);
        requester.setId(10L);

        procurementUser = new User("po_buyer", "buyer@smartprocure.io", "pass", "Buyer", Role.PROCUREMENT);
        procurementUser.setId(20L);

        department = new Department("OPS", "Operations", "Operations Dept");
        department.setId(5L);

        supplier = new Supplier("Apex Industrial", "sales@apex.com", "NET_30");
        supplier.setId(50L);
        supplier.setActive(true);

        approvedRequest = new PurchaseRequest("PR-20261014-001", "Factory Machinery", requester, department);
        approvedRequest.setId(100L);
        approvedRequest.setStatus(RequestStatus.APPROVED);
        approvedRequest.setTotalAmount(new BigDecimal("4500.00"));

        PurchaseRequestItem item = new PurchaseRequestItem("Hydraulic Pump", new BigDecimal("1.00"), new BigDecimal("4500.00"));
        approvedRequest.addLineItem(item);
    }

    @Test
    @DisplayName("Generate PO: Successfully creates purchase order from approved requisition and commits budget spend")
    void testGenerateFromApprovedRequest_Success() {
        GeneratePurchaseOrderRequest req = new GeneratePurchaseOrderRequest(50L, "Standard delivery");

        when(requestRepository.findById(100L)).thenReturn(Optional.of(approvedRequest));
        when(userRepository.findByEmail("buyer@smartprocure.io")).thenReturn(Optional.of(procurementUser));
        when(orderRepository.findByPurchaseRequestId(100L)).thenReturn(Optional.empty());
        when(supplierRepository.findById(50L)).thenReturn(Optional.of(supplier));
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            po.setId(1000L);
            return po;
        });

        PurchaseOrderResponse response = purchaseOrderService.generateFromApprovedRequest(100L, req, "buyer@smartprocure.io");

        assertNotNull(response);
        assertEquals(POStatus.ISSUED, response.getStatus());
        assertEquals("Apex Industrial", response.getVendorName());
        assertEquals(RequestStatus.PO_CREATED, approvedRequest.getStatus());
        verify(budgetService, times(1)).commitSpend(eq(5L), anyInt(), argThat(b -> b != null && b.compareTo(new BigDecimal("4500.00")) == 0));
        verify(approvalHistoryRepository, times(1)).save(any(ApprovalHistory.class));
    }

    @Test
    @DisplayName("Generate PO: Fails with BadRequestException when requisition is not in APPROVED status")
    void testGenerateFromUnapprovedRequest_Fails() {
        approvedRequest.setStatus(RequestStatus.PENDING_MANAGER_APPROVAL);
        GeneratePurchaseOrderRequest req = new GeneratePurchaseOrderRequest(50L, "Standard delivery");

        when(requestRepository.findById(100L)).thenReturn(Optional.of(approvedRequest));
        when(userRepository.findByEmail("buyer@smartprocure.io")).thenReturn(Optional.of(procurementUser));

        assertThrows(BadRequestException.class, () ->
                purchaseOrderService.generateFromApprovedRequest(100L, req, "buyer@smartprocure.io"));
    }

    @Test
    @DisplayName("Generate PO: Fails when supplier is inactive")
    void testGenerateWithInactiveSupplier_Fails() {
        supplier.setActive(false);
        GeneratePurchaseOrderRequest req = new GeneratePurchaseOrderRequest(50L, "Standard delivery");

        when(requestRepository.findById(100L)).thenReturn(Optional.of(approvedRequest));
        when(userRepository.findByEmail("buyer@smartprocure.io")).thenReturn(Optional.of(procurementUser));
        when(supplierRepository.findById(50L)).thenReturn(Optional.of(supplier));

        assertThrows(BadRequestException.class, () ->
                purchaseOrderService.generateFromApprovedRequest(100L, req, "buyer@smartprocure.io"));
    }

    @Test
    @DisplayName("Update PO Status: Only procurement or admin can update status")
    void testUpdateStatus_UnauthorizedUser() {
        PurchaseOrder po = new PurchaseOrder("PO-20261014-1001", "Test Order", "Apex Industrial", new BigDecimal("100.00"));
        po.setId(200L);

        when(orderRepository.findById(200L)).thenReturn(Optional.of(po));
        when(userRepository.findByEmail("requester@smartprocure.io")).thenReturn(Optional.of(requester));

        UpdatePOStatusRequest updateReq = new UpdatePOStatusRequest(POStatus.FULFILLED, "Delivered");

        assertThrows(AccessDeniedException.class, () ->
                purchaseOrderService.updateOrderStatus(200L, updateReq, "requester@smartprocure.io"));
    }
}
