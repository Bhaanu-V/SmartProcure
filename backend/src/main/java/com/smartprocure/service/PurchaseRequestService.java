package com.smartprocure.service;

import com.smartprocure.dto.pr.*;
import com.smartprocure.entity.*;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.CatalogItemRepository;
import com.smartprocure.repository.DepartmentRepository;
import com.smartprocure.repository.PurchaseRequestRepository;
import com.smartprocure.repository.UserRepository;
import com.smartprocure.validator.PurchaseRequestCalculationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service managing purchase request draft lifecycle, line items, and access permissions.
 */
@Service
public class PurchaseRequestService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseRequestService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final PurchaseRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final PurchaseRequestCalculationValidator calculationValidator;

    public PurchaseRequestService(PurchaseRequestRepository requestRepository,
                                  UserRepository userRepository,
                                  DepartmentRepository departmentRepository,
                                  CatalogItemRepository catalogItemRepository,
                                  PurchaseRequestCalculationValidator calculationValidator) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.calculationValidator = calculationValidator;
    }

    /**
     * Creates a new purchase requisition in DRAFT status with server-calculated line totals.
     */
    @Transactional
    public PurchaseRequestResponse createDraft(CreatePurchaseRequest request, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", requesterEmail));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));

        String requestNumber = generateUniqueRequestNumber();

        PurchaseRequest pr = new PurchaseRequest();
        pr.setRequestNumber(requestNumber);
        pr.setTitle(request.getTitle().trim());
        pr.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        pr.setDepartment(department);
        pr.setRequester(requester);
        pr.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        pr.setCurrency(request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "USD");
        pr.setStatus(RequestStatus.DRAFT);

        List<PurchaseRequestItem> items = buildLineItems(request.getLineItems(), pr);
        pr.setLineItems(items);

        BigDecimal calculatedTotal = calculationValidator.calculateGrandTotal(request.getLineItems());
        pr.setTotalAmount(calculatedTotal);

        PurchaseRequest saved = requestRepository.save(pr);
        log.info("Created PR draft '{}' ({}) by user '{}' with total {}",
                saved.getRequestNumber(), saved.getTitle(), requesterEmail, saved.getTotalAmount());

        return PurchaseRequestResponse.fromEntity(saved);
    }

    /**
     * Updates an existing draft purchase request. Only permitted if status is DRAFT.
     */
    @Transactional
    public PurchaseRequestResponse updateDraft(Long id, UpdatePurchaseRequest request, String userEmail) {
        PurchaseRequest pr = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", id));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        checkCanModify(pr, currentUser);

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            pr.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            pr.setDescription(request.getDescription().trim());
        }
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            pr.setDepartment(department);
        }
        if (request.getPriority() != null) {
            pr.setPriority(request.getPriority());
        }

        if (request.getLineItems() != null && !request.getLineItems().isEmpty()) {
            pr.getLineItems().clear();
            List<PurchaseRequestItem> newItems = buildLineItems(request.getLineItems(), pr);
            pr.getLineItems().addAll(newItems);

            BigDecimal calculatedTotal = calculationValidator.calculateGrandTotal(request.getLineItems());
            pr.setTotalAmount(calculatedTotal);
        }

        PurchaseRequest saved = requestRepository.save(pr);
        log.info("Updated PR draft '{}' by user '{}'. New total: {}",
                saved.getRequestNumber(), userEmail, saved.getTotalAmount());

        return PurchaseRequestResponse.fromEntity(saved);
    }

    /**
     * Retrieves a purchase request by ID with role-based access validation.
     */
    @Transactional(readOnly = true)
    public PurchaseRequestResponse getPurchaseRequestById(Long id, String userEmail) {
        PurchaseRequest pr = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", id));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        checkCanAccess(pr, currentUser);
        return PurchaseRequestResponse.fromEntity(pr);
    }

    /**
     * Retrieves a purchase request by human-readable request number.
     */
    @Transactional(readOnly = true)
    public PurchaseRequestResponse getPurchaseRequestByNumber(String requestNumber, String userEmail) {
        PurchaseRequest pr = requestRepository.findByRequestNumber(requestNumber)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "requestNumber", requestNumber));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        checkCanAccess(pr, currentUser);
        return PurchaseRequestResponse.fromEntity(pr);
    }

    /**
     * Deletes a draft purchase request. Only owner or ADMIN can delete drafts.
     */
    @Transactional
    public void deleteDraft(Long id, String userEmail) {
        PurchaseRequest pr = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", id));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        checkCanModify(pr, currentUser);
        requestRepository.delete(pr);
        log.info("Deleted PR draft '{}' by user '{}'", pr.getRequestNumber(), userEmail);
    }

    /**
     * Retrieves current user's submitted and draft purchase requests.
     */
    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> getMyRequests(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return requestRepository.findByRequesterId(user.getId(), pageable)
                .map(PurchaseRequestResponse::fromEntity);
    }

    private List<PurchaseRequestItem> buildLineItems(List<CreateLineItemRequest> itemRequests, PurchaseRequest pr) {
        List<PurchaseRequestItem> items = new ArrayList<>();
        for (CreateLineItemRequest req : itemRequests) {
            BigDecimal lineTotal = calculationValidator.validateAndCalculateLineTotal(req);

            PurchaseRequestItem item = new PurchaseRequestItem();
            item.setPurchaseRequest(pr);
            item.setItemDescription(req.getItemDescription().trim());
            item.setCategory(req.getCategory() != null ? req.getCategory().trim().toUpperCase() : null);
            item.setQuantity(req.getQuantity());
            item.setUnitPrice(req.getUnitPrice());

            if (req.getCatalogItemId() != null) {
                CatalogItem catalogItem = catalogItemRepository.findById(req.getCatalogItemId())
                        .orElse(null);
                item.setCatalogItem(catalogItem);
            }

            items.add(item);
        }
        return items;
    }

    private String generateUniqueRequestNumber() {
        String datePrefix = LocalDate.now().format(DATE_FORMATTER);
        String number;
        int attempts = 0;
        do {
            int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
            number = String.format("PR-%s-%04d", datePrefix, randomSuffix);
            attempts++;
            if (attempts > 50) {
                number = String.format("PR-%s-%d", datePrefix, System.currentTimeMillis() % 100000);
                break;
            }
        } while (requestRepository.existsByRequestNumber(number));
        return number;
    }

    private void checkCanModify(PurchaseRequest pr, User user) {
        if (pr.getStatus() != RequestStatus.DRAFT) {
            throw new BadRequestException("Cannot modify purchase request in status: " + pr.getStatus());
        }

        boolean isOwner = pr.getRequester().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to modify this purchase request");
        }
    }

    private void checkCanAccess(PurchaseRequest pr, User user) {
        boolean isOwner = pr.getRequester().getId().equals(user.getId());
        boolean isManager = pr.getDepartment() != null && pr.getDepartment().getManager() != null &&
                pr.getDepartment().getManager().getId().equals(user.getId());
        boolean hasPrivilegedRole = user.getRole() == Role.ADMIN ||
                user.getRole() == Role.FINANCE ||
                user.getRole() == Role.PROCUREMENT;

        if (!isOwner && !isManager && !hasPrivilegedRole) {
            throw new AccessDeniedException("You do not have permission to view this purchase request");
        }
    }
}
