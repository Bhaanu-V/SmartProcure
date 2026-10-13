package com.smartprocure.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartprocure.dto.pr.CreateLineItemRequest;
import com.smartprocure.dto.pr.CreatePurchaseRequest;
import com.smartprocure.dto.pr.UpdatePurchaseRequest;
import com.smartprocure.entity.Department;
import com.smartprocure.entity.Role;
import com.smartprocure.entity.User;
import com.smartprocure.repository.ApprovalHistoryRepository;
import com.smartprocure.repository.DepartmentRepository;
import com.smartprocure.repository.PurchaseRequestRepository;
import com.smartprocure.repository.UserRepository;
import com.smartprocure.security.jwt.JwtTokenProvider;
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
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PurchaseRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PurchaseRequestRepository requestRepository;

    @Autowired
    private ApprovalHistoryRepository approvalHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testRequester;
    private Department testDept;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testDept = new Department("QA_DEPT", "Quality Assurance", "Testing Department");
        testDept = departmentRepository.save(testDept);

        testRequester = new User("pr_tester", "pr_tester@smartprocure.io",
                passwordEncoder.encode("Password123!"), "PR Tester", Role.EMPLOYEE);
        testRequester = userRepository.save(testRequester);

        jwtToken = jwtTokenProvider.generateToken(testRequester.getEmail(), testRequester.getId(), testRequester.getRole().name());
    }

    @AfterEach
    void tearDown() {
        if (testRequester != null && testRequester.getId() != null) {
            requestRepository.findByRequesterId(testRequester.getId(), org.springframework.data.domain.Pageable.unpaged())
                    .forEach(pr -> {
                        approvalHistoryRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(pr.getId())
                                .forEach(approvalHistoryRepository::delete);
                        requestRepository.delete(pr);
                    });
            userRepository.delete(testRequester);
        }
        if (testDept != null && testDept.getId() != null) {
            departmentRepository.delete(testDept);
        }
    }

    @Test
    @DisplayName("End-to-end purchase request lifecycle: create draft, update, submit, query, cancel")
    void testPurchaseRequestLifecycle() throws Exception {
        // 1. Create Draft
        CreateLineItemRequest item = new CreateLineItemRequest("Ergonomic Keyboard", new BigDecimal("2.00"), new BigDecimal("75.50"));
        CreatePurchaseRequest createReq = new CreatePurchaseRequest(
                "Engineering Accessories", testDept.getId(), Collections.singletonList(item));

        MvcResult createResult = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(151.00))
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long prId = createdNode.get("id").asLong();

        // 2. Update Draft
        UpdatePurchaseRequest updateReq = new UpdatePurchaseRequest();
        updateReq.setTitle("Updated Engineering Accessories");

        mockMvc.perform(put("/api/requests/" + prId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Engineering Accessories"));

        // 3. Submit Requisition
        mockMvc.perform(post("/api/requests/" + prId + "/submit")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_MANAGER_APPROVAL"));

        // 4. Retrieve by ID
        mockMvc.perform(get("/api/requests/" + prId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prId))
                .andExpect(jsonPath("$.status").value("PENDING_MANAGER_APPROVAL"));

        // 5. Query My Requests
        mockMvc.perform(get("/api/requests/my")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        // 6. Cancel Requisition
        mockMvc.perform(post("/api/requests/" + prId + "/cancel")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("reason", "Budget reprioritized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
