package com.smartprocure.service;

import com.smartprocure.audit.Auditable;
import com.smartprocure.dto.po.GeneratePurchaseOrderRequest;
import com.smartprocure.dto.po.PurchaseOrderResponse;
import com.smartprocure.dto.po.UpdatePOStatusRequest;
import com.smartprocure.entity.*;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service managing Purchase Order creation from approved requisitions,
 * budget spend commitment, order lifecycle transitions, and document authorization.
 */
@Service
public class PurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseRequestRepository requestRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final NotificationRepository notificationRepository;

    public PurchaseOrderService(PurchaseOrderRepository orderRepository,
                                PurchaseRequestRepository requestRepository,
                                SupplierRepository supplierRepository,
                                UserRepository userRepository,
                                BudgetService budgetService,
                                ApprovalHistoryRepository approvalHistoryRepository,
                                NotificationRepository notificationRepository) {
        this.orderRepository = orderRepository;
        this.requestRepository = requestRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.budgetService = budgetService;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Issues an official Purchase Order from a fully approved Purchase Request.
     * Commits the reserved department budget to spent and notifies stakeholders.
     */
    @Auditable(action = "PO_GENERATED", entityName = "PurchaseOrder")
    @Transactional
    public PurchaseOrderResponse generateFromApprovedRequest(Long requestId,
                                                            GeneratePurchaseOrderRequest request,
                                                            String actorPrincipal) {
        PurchaseRequest pr = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", requestId));

        User actor = findUserByPrincipal(actorPrincipal);

        if (pr.getStatus() != RequestStatus.APPROVED) {
            throw new BadRequestException(String.format(
                    "Cannot generate purchase order for request '%s' in status %s. Requisition must be in APPROVED status.",
                    pr.getRequestNumber(), pr.getStatus()));
        }

        if (orderRepository.findByPurchaseRequestId(requestId).isPresent()) {
            throw new BadRequestException("A purchase order has already been issued for requisition: " + pr.getRequestNumber());
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        if (!supplier.isActive()) {
            throw new BadRequestException("Cannot assign purchase order to inactive supplier: " + supplier.getName());
        }

        String poNumber = generateUniquePoNumber();

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(poNumber);
        po.setTitle(pr.getTitle());
        po.setDescription(request.getDeliveryNotes() != null && !request.getDeliveryNotes().isBlank()
                ? request.getDeliveryNotes().trim() : pr.getDescription());
        po.setVendorName(supplier.getName());
        po.setSupplier(supplier);
        po.setPurchaseRequest(pr);
        po.setDepartment(pr.getDepartment());
        po.setRequester(pr.getRequester());
        po.setCurrency(pr.getCurrency() != null ? pr.getCurrency() : "USD");
        po.setTotalAmount(pr.getTotalAmount());
        po.setStatus(POStatus.ISSUED);
        po.setPriority(pr.getPriority());

        // Copy requisition line items into purchase order items
        if (pr.getLineItems() != null) {
            for (PurchaseRequestItem prItem : pr.getLineItems()) {
                PurchaseOrderItem poItem = new PurchaseOrderItem(
                        prItem.getItemDescription(), prItem.getQuantity(), prItem.getUnitPrice());
                poItem.setCategory(prItem.getCategory());
                po.addLineItem(poItem);
            }
        }

        PurchaseOrder savedPo = orderRepository.save(po);

        // Transition PR status to PO_CREATED
        pr.setStatus(RequestStatus.PO_CREATED);
        requestRepository.save(pr);

        // Commit department budget expense from reserved to spent
        if (pr.getDepartment() != null) {
            int fiscalYear = pr.getCreatedAt() != null ? pr.getCreatedAt().getYear() : LocalDate.now().getYear();
            try {
                budgetService.commitSpend(pr.getDepartment().getId(), fiscalYear, pr.getTotalAmount());
            } catch (Exception ex) {
                log.warn("Budget spend commitment warning for department {}: {}", pr.getDepartment().getName(), ex.getMessage());
            }
        }

        // Record approval history
        ApprovalHistory history = new ApprovalHistory(
                savedPo, actor, ApprovalAction.APPROVED,
                "Purchase Order issued to supplier " + supplier.getName());
        approvalHistoryRepository.save(history);

        // Notify requester
        if (pr.getRequester() != null) {
            Notification notification = new Notification(
                    pr.getRequester(),
                    "Purchase Order Issued: " + savedPo.getPoNumber(),
                    String.format("Purchase Order %s ($%s) has been officially issued to supplier %s for your request '%s'.",
                            savedPo.getPoNumber(), savedPo.getTotalAmount(), supplier.getName(), pr.getTitle()),
                    NotificationType.INFO,
                    "PURCHASE_ORDER",
                    savedPo.getId()
            );
            notificationRepository.save(notification);
        }

        log.info("Successfully generated Purchase Order '{}' for requisition '{}' by actor '{}'",
                savedPo.getPoNumber(), pr.getRequestNumber(), actor.getUsername());

        return PurchaseOrderResponse.fromEntity(savedPo);
    }

    /**
     * Retrieves a purchase order by ID with role-based authorization check.
     */
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long id, String userPrincipal) {
        PurchaseOrder po = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        User currentUser = findUserByPrincipal(userPrincipal);
        checkCanAccess(po, currentUser);

        return PurchaseOrderResponse.fromEntity(po);
    }

    /**
     * Retrieves a purchase order by unique PO number.
     */
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderByNumber(String poNumber, String userPrincipal) {
        PurchaseOrder po = orderRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "poNumber", poNumber));

        User currentUser = findUserByPrincipal(userPrincipal);
        checkCanAccess(po, currentUser);

        return PurchaseOrderResponse.fromEntity(po);
    }

    /**
     * Filters purchase orders by supplier, status, or search query.
     */
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> filterOrders(Long supplierId, POStatus status, String query, Pageable pageable) {
        return orderRepository.filterOrders(supplierId, status, query, pageable)
                .map(PurchaseOrderResponse::fromEntity);
    }

    /**
     * Updates the status lifecycle of an active purchase order.
     */
    @Auditable(action = "PO_STATUS_UPDATED", entityName = "PurchaseOrder")
    @Transactional
    public PurchaseOrderResponse updateOrderStatus(Long id, UpdatePOStatusRequest request, String userPrincipal) {
        PurchaseOrder po = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        User currentUser = findUserByPrincipal(userPrincipal);

        boolean isProcurement = currentUser.getRole() == Role.PROCUREMENT;
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isProcurement && !isAdmin) {
            throw new AccessDeniedException("Only procurement officers or administrators can update purchase order status");
        }

        POStatus oldStatus = po.getStatus();
        po.setStatus(request.getStatus());
        PurchaseOrder updated = orderRepository.save(po);

        log.info("Updated Purchase Order '{}' status from {} to {} by user '{}'",
                po.getPoNumber(), oldStatus, request.getStatus(), currentUser.getUsername());

        return PurchaseOrderResponse.fromEntity(updated);
    }

    public PurchaseOrder getEntityForExport(Long id, String userPrincipal) {
        PurchaseOrder po = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        User currentUser = findUserByPrincipal(userPrincipal);
        checkCanAccess(po, currentUser);

        return po;
    }

    private void checkCanAccess(PurchaseOrder po, User user) {
        boolean isOwner = po.getRequester() != null && po.getRequester().getId().equals(user.getId());
        boolean isDeptManager = po.getDepartment() != null && po.getDepartment().getManager() != null &&
                po.getDepartment().getManager().getId().equals(user.getId());
        boolean hasPrivilegedRole = user.getRole() == Role.ADMIN ||
                user.getRole() == Role.FINANCE ||
                user.getRole() == Role.PROCUREMENT;

        if (!isOwner && !isDeptManager && !hasPrivilegedRole) {
            throw new AccessDeniedException("You do not have authorization to access purchase order: " + po.getPoNumber());
        }
    }

    private String generateUniquePoNumber() {
        String datePrefix = LocalDate.now().format(DATE_FORMATTER);
        String number;
        int attempts = 0;
        do {
            int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
            number = String.format("PO-%s-%04d", datePrefix, randomSuffix);
            attempts++;
            if (attempts > 50) {
                number = String.format("PO-%s-%d", datePrefix, System.currentTimeMillis() % 100000);
                break;
            }
        } while (orderRepository.existsByPoNumber(number));
        return number;
    }

    private User findUserByPrincipal(String principal) {
        return userRepository.findByEmail(principal)
                .or(() -> userRepository.findByUsername(principal))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username or email", principal));
    }
}
