package com.smartprocure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartprocure.dto.approval.ApprovalDecisionRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ApprovalControllerIntegrationTest {

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
    private PurchaseRequestRepository requestRepository;

    @Autowired
    private ApprovalHistoryRepository approvalHistoryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PurchaseRequestService purchaseRequestService;

    private User requester;
    private User manager;
    private User financeUser;
    private Department department;
    private DepartmentBudget budget;

    private String requesterToken;
    private String managerToken;
    private String financeToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        department = new Department("APPR_DEPT", "Approval Testing Dept", "Dept for testing approval tiers");
        department = departmentRepository.save(department);

        manager = new User("appr_mgr", "appr_mgr@smartprocure.io",
                passwordEncoder.encode("Password123!"), "Approval Manager", Role.MANAGER);
        manager.setDepartmentEntity(department);
        manager = userRepository.save(manager);

        department.setManager(manager);
        department = departmentRepository.save(department);

        requester = new User("appr_emp", "appr_emp@smartprocure.io",
                passwordEncoder.encode("Password123!"), "Approval Requester", Role.EMPLOYEE);
        requester.setDepartmentEntity(department);
        requester = userRepository.save(requester);

        financeUser = new User("appr_fin", "appr_fin@smartprocure.io",
                passwordEncoder.encode("Password123!"), "Approval Finance", Role.FINANCE);
        financeUser = userRepository.save(financeUser);

        budget = new DepartmentBudget(department, LocalDate.now().getYear(), new BigDecimal("50000.00"));
        budget = budgetRepository.save(budget);

        requesterToken = jwtTokenProvider.generateToken(requester.getEmail(), requester.getId(), requester.getRole().name());
        managerToken = jwtTokenProvider.generateToken(manager.getEmail(), manager.getId(), manager.getRole().name());
        financeToken = jwtTokenProvider.generateToken(financeUser.getEmail(), financeUser.getId(), financeUser.getRole().name());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        departmentRepository.findByCode("APPR_DEPT").ifPresent(d -> {
            budgetRepository.findByDepartmentId(d.getId()).forEach(b -> budgetRepository.deleteById(b.getId()));
            userRepository.findByDepartmentId(d.getId()).forEach(u -> {
                requestRepository.findByRequesterId(u.getId(), org.springframework.data.domain.Pageable.unpaged())
                        .forEach(pr -> {
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
        userRepository.findByUsername("appr_mgr").ifPresent(userRepository::delete);
        userRepository.findByUsername("appr_emp").ifPresent(userRepository::delete);
        userRepository.findByUsername("appr_fin").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("End-to-end multi-tier approval workflow: Self-approval prevention, manager approval, finance approval, and budget reservation")
    void testEndToEndApprovalWorkflow() throws Exception {
        // 1. Create and submit PR
        CreateLineItemRequest item = new CreateLineItemRequest("Dev Laptop", new BigDecimal("1.00"), new BigDecimal("1200.00"));
        CreatePurchaseRequest createReq = new CreatePurchaseRequest("Q4 Hardware", department.getId(), Collections.singletonList(item));
        var prResponse = purchaseRequestService.createDraft(createReq, requester.getEmail());
        var submittedPr = purchaseRequestService.submitPurchaseRequest(prResponse.getId(), requester.getEmail());
        Long prId = submittedPr.getId();

        // 2. Self-approval blocked: Requester attempts to approve via manager endpoint
        ApprovalDecisionRequest managerDecision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Self approving");
        mockMvc.perform(post("/api/approvals/" + prId + "/manager")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerDecision)))
                .andExpect(status().isForbidden());

        // 3. Manager approves requisition
        ApprovalDecisionRequest validManagerDecision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Manager approves requisition");
        mockMvc.perform(post("/api/approvals/" + prId + "/manager")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validManagerDecision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_FINANCE_APPROVAL"));

        // 4. Unauthorized finance approval: Manager attempts to approve finance stage
        ApprovalDecisionRequest financeDecision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Manager approving finance");
        mockMvc.perform(post("/api/approvals/" + prId + "/finance")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(financeDecision)))
                .andExpect(status().isForbidden());

        // 5. Finance officer approves requisition and reserves budget
        ApprovalDecisionRequest validFinanceDecision = new ApprovalDecisionRequest(ApprovalAction.APPROVED, "Budget verified and approved");
        mockMvc.perform(post("/api/approvals/" + prId + "/finance")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFinanceDecision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 6. Verify department budget was correctly reserved
        DepartmentBudget updatedBudget = budgetRepository.findByDepartmentIdAndFiscalYear(department.getId(), LocalDate.now().getYear())
                .orElseThrow();
        assertEquals(0, updatedBudget.getReservedAmount().compareTo(new BigDecimal("1200.00")));
        assertEquals(0, updatedBudget.getAvailableAmount().compareTo(new BigDecimal("48800.00")));

        // 7. Verify approval history records
        mockMvc.perform(get("/api/approvals/history/" + prId)
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))) // Submission + Manager Approval + Finance Approval
                .andExpect(jsonPath("$[0].action").value("APPROVED"))
                .andExpect(jsonPath("$[0].stage").value("FINANCE"))
                .andExpect(jsonPath("$[1].action").value("APPROVED"))
                .andExpect(jsonPath("$[1].stage").value("MANAGER"));
    }
}
