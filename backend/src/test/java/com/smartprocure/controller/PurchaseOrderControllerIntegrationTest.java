package com.smartprocure.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartprocure.dto.approval.ApprovalDecisionRequest;
import com.smartprocure.dto.po.GeneratePurchaseOrderRequest;
import com.smartprocure.dto.po.UpdatePOStatusRequest;
import com.smartprocure.dto.pr.CreateLineItemRequest;
import com.smartprocure.dto.pr.CreatePurchaseRequest;
import com.smartprocure.entity.*;
import com.smartprocure.repository.*;
import com.smartprocure.security.jwt.JwtTokenProvider;
import com.smartprocure.service.PurchaseRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PurchaseOrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentBudgetRepository budgetRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PurchaseRequestRepository requestRepository;

    @Autowired
    private PurchaseOrderRepository orderRepository;

    @Autowired
    private ApprovalHistoryRepository approvalHistoryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PurchaseRequestService purchaseRequestService;

    private User requester;
    private User manager;
    private User financeUser;
    private User procurementUser;
    private User unauthorizedUser;
    private Department department;
    private DepartmentBudget budget;
    private Supplier supplier;

    private String requesterToken;
    private String managerToken;
    private String financeToken;
    private String procurementToken;
    private String unauthorizedToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        department = new Department("PO_TEST_DEPT", "PO Testing Department", "Testing PO issuance");
        department = departmentRepository.save(department);

        manager = new User("po_mgr", "po_mgr@smartprocure.io",
                passwordEncoder.encode("Password123!"), "PO Manager", Role.MANAGER);
        manager.setDepartmentEntity(department);
        manager = userRepository.save(manager);

        department.setManager(manager);
        department = departmentRepository.save(department);

        requester = new User("po_emp", "po_emp@smartprocure.io",
                passwordEncoder.encode("Password123!"), "PO Requester", Role.EMPLOYEE);
        requester.setDepartmentEntity(department);
        requester = userRepository.save(requester);

        financeUser = new User("po_fin", "po_fin@smartprocure.io",
                passwordEncoder.encode("Password123!"), "PO Finance", Role.FINANCE);
        financeUser = userRepository.save(financeUser);

        procurementUser = new User("po_buyer", "po_buyer@smartprocure.io",
                passwordEncoder.encode("Password123!"), "PO Buyer", Role.PROCUREMENT);
        procurementUser = userRepository.save(procurementUser);

        unauthorizedUser = new User("po_intruder", "po_intruder@smartprocure.io",
                passwordEncoder.encode("Password123!"), "PO Intruder", Role.EMPLOYEE);
        unauthorizedUser = userRepository.save(unauthorizedUser);

        budget = new DepartmentBudget(department, LocalDate.now().getYear(), new BigDecimal("100000.00"));
        budget = budgetRepository.save(budget);

        supplier = new Supplier("Apex Precision Hardware", "sales@apexhardware.com", "NET_30");
        supplier.setContactPhone("+1-555-4321");
        supplier.setAddress("100 Industrial Parkway");
        supplier = supplierRepository.save(supplier);

        requesterToken = jwtTokenProvider.generateToken(requester.getEmail(), requester.getId(), requester.getRole().name());
        managerToken = jwtTokenProvider.generateToken(manager.getEmail(), manager.getId(), manager.getRole().name());
        financeToken = jwtTokenProvider.generateToken(financeUser.getEmail(), financeUser.getId(), financeUser.getRole().name());
        procurementToken = jwtTokenProvider.generateToken(procurementUser.getEmail(), procurementUser.getId(), procurementUser.getRole().name());
        unauthorizedToken = jwtTokenProvider.generateToken(unauthorizedUser.getEmail(), unauthorizedUser.getId(), unauthorizedUser.getRole().name());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        supplierRepository.findByName("Apex Precision Hardware").ifPresent(s -> {
            orderRepository.findBySupplierId(s.getId(), org.springframework.data.domain.Pageable.unpaged())
                    .forEach(po -> {
                        approvalHistoryRepository.findByPurchaseOrderIdOrderByCreatedAtDesc(po.getId())
                                .forEach(approvalHistoryRepository::delete);
                        orderRepository.delete(po);
                    });
            supplierRepository.delete(s);
        });

        departmentRepository.findByCode("PO_TEST_DEPT").ifPresent(d -> {
            budgetRepository.findByDepartmentId(d.getId()).forEach(b -> budgetRepository.deleteById(b.getId()));
            userRepository.findByDepartmentId(d.getId()).forEach(u -> {
                requestRepository.findByRequesterId(u.getId(), org.springframework.data.domain.Pageable.unpaged())
                        .forEach(pr -> {
                            orderRepository.findByPurchaseRequestId(pr.getId()).ifPresent(po -> {
                                approvalHistoryRepository.findByPurchaseOrderIdOrderByCreatedAtDesc(po.getId())
                                        .forEach(approvalHistoryRepository::delete);
                                orderRepository.delete(po);
                            });
                            approvalHistoryRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(pr.getId())
                                    .forEach(approvalHistoryRepository::delete);
                            requestRepository.delete(pr);
                        });
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(u.getId())
                        .forEach(notificationRepository::delete);
                userRepository.delete(u);
            });
            d.setManager(null);
            departmentRepository.save(d);
            departmentRepository.delete(d);
        });

        userRepository.findByUsername("po_mgr").ifPresent(userRepository::delete);
        userRepository.findByUsername("po_emp").ifPresent(userRepository::delete);
        userRepository.findByUsername("po_fin").ifPresent(userRepository::delete);
        userRepository.findByUsername("po_buyer").ifPresent(userRepository::delete);
        userRepository.findByUsername("po_intruder").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("End-to-end Purchase Order lifecycle: Approved requisition -> PO issuance -> Status update -> PDF export & Authorization check")
    void testPurchaseOrderLifecycleAndPdfExport() throws Exception {
        // 1. Create and submit PR
        CreateLineItemRequest item = new CreateLineItemRequest("Industrial Router", new BigDecimal("2.00"), new BigDecimal("1500.00"));
        CreatePurchaseRequest createReq = new CreatePurchaseRequest("Q4 Network Upgrades", department.getId(), Collections.singletonList(item));
        var prResponse = purchaseRequestService.createDraft(createReq, requester.getEmail());
        var submittedPr = purchaseRequestService.submitPurchaseRequest(prResponse.getId(), requester.getEmail());
        Long prId = submittedPr.getId();

        // 2. Manager approval
        ApprovalDecisionRequest managerDecision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Manager approves network upgrade");
        mockMvc.perform(post("/api/approvals/" + prId + "/manager")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerDecision)))
                .andExpect(status().isOk());

        // 3. Finance approval (reserves budget)
        ApprovalDecisionRequest financeDecision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Budget verified and approved");
        mockMvc.perform(post("/api/approvals/" + prId + "/finance")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(financeDecision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 4. Procurement generates Purchase Order
        GeneratePurchaseOrderRequest generateReq = new GeneratePurchaseOrderRequest(supplier.getId(), "Rush delivery to Data Center 2");
        MvcResult poResult = mockMvc.perform(post("/api/orders/from-request/" + prId)
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generateReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.vendorName").value("Apex Precision Hardware"))
                .andExpect(jsonPath("$.totalAmount").value(3000.00))
                .andExpect(jsonPath("$.lineItems[0].itemDescription").value("Industrial Router"))
                .andReturn();

        JsonNode poJson = objectMapper.readTree(poResult.getResponse().getContentAsString());
        long poId = poJson.get("id").asLong();
        String poNumber = poJson.get("poNumber").asText();
        assertNotNull(poNumber);

        // 5. Query PO by ID
        mockMvc.perform(get("/api/orders/" + poId)
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poNumber").value(poNumber));

        // 6. Query PO by Number
        mockMvc.perform(get("/api/orders/number/" + poNumber)
                        .header("Authorization", "Bearer " + procurementToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(poId));

        // 7. Procurement updates status to FULFILLED
        UpdatePOStatusRequest statusUpdate = new UpdatePOStatusRequest(POStatus.FULFILLED, "Delivered and verified by warehouse");
        mockMvc.perform(put("/api/orders/" + poId + "/status")
                        .header("Authorization", "Bearer " + procurementToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));

        // 8. Download PDF document (Authorized requester and procurement)
        MvcResult pdfResult = mockMvc.perform(get("/api/orders/" + poId + "/pdf")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"" + poNumber + ".pdf\""))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);

        // 9. Unauthorized PDF access blocked (403 Forbidden)
        mockMvc.perform(get("/api/orders/" + poId + "/pdf")
                        .header("Authorization", "Bearer " + unauthorizedToken))
                .andExpect(status().isForbidden());
    }
}
