package com.smartprocure.ai;

import com.smartprocure.ai.client.GeminiClient;
import com.smartprocure.ai.dto.AiRiskAssessmentResponse;
import com.smartprocure.ai.dto.RiskLevel;
import com.smartprocure.ai.service.ProcurementAiService;
import com.smartprocure.entity.*;
import com.smartprocure.repository.PurchaseOrderRepository;
import com.smartprocure.repository.PurchaseRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcurementAiService Anomaly Detection & AI Assessment Tests")
class ProcurementAiServiceTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private PurchaseRequestRepository requestRepository;

    @Mock
    private PurchaseOrderRepository orderRepository;

    private ProcurementAiService aiService;

    private Department department;
    private User requester;

    @BeforeEach
    void setUp() {
        aiService = new ProcurementAiService(geminiClient, requestRepository, orderRepository);

        department = new Department();
        department.setId(10L);
        department.setName("Information Technology");
        department.setCode("IT");

        requester = new User();
        requester.setId(1L);
        requester.setUsername("requester_test");
        requester.setEmail("requester@smartprocure.io");
    }

    @Test
    @DisplayName("Should assess routine low spend request as LOW risk with deterministic summary")
    void testAnalyzeRoutineLowSpendRequest() {
        PurchaseRequest pr = new PurchaseRequest();
        pr.setId(101L);
        pr.setRequestNumber("PR-20261014-0101");
        pr.setTitle("Office Stationery");
        pr.setTotalAmount(new BigDecimal("450.00"));
        pr.setStatus(RequestStatus.PENDING_MANAGER_APPROVAL);
        pr.setDepartment(department);
        pr.setRequester(requester);

        PurchaseRequestItem item = new PurchaseRequestItem("Notebooks and pens", new BigDecimal("10"), new BigDecimal("45.00"));
        pr.setLineItems(List.of(item));

        when(requestRepository.findById(101L)).thenReturn(Optional.of(pr));
        when(requestRepository.findByDepartmentId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pr)));
        when(geminiClient.generateText(anyString())).thenReturn(Optional.empty());

        AiRiskAssessmentResponse resp = aiService.analyzePurchaseRequest(101L);

        assertNotNull(resp);
        assertEquals(RiskLevel.LOW, resp.getRiskLevel());
        assertEquals("ROUTINE_APPROVAL_ELIGIBLE", resp.getRecommendation());
        assertFalse(resp.isAiGenerated());
        assertTrue(resp.getDetectedFlags().isEmpty());
        assertTrue(resp.getAnalysisSummary().contains("complies with routine procurement thresholds"));
        assertNotNull(resp.getDisclaimer());

        // Verify entity updated and saved
        verify(requestRepository).save(argThat(saved ->
                saved.getAiRiskScore().compareTo(new BigDecimal("10.00")) == 0 &&
                saved.getAiRecommendation().equals("ROUTINE_APPROVAL_ELIGIBLE") &&
                saved.getStatus() == RequestStatus.PENDING_MANAGER_APPROVAL // Status untouched!
        ));
    }

    @Test
    @DisplayName("Should detect critical spend threshold (> $50,000) and flag as HIGH risk")
    void testAnalyzeCriticalSpendRequest() {
        PurchaseRequest pr = new PurchaseRequest();
        pr.setId(102L);
        pr.setRequestNumber("PR-20261014-0102");
        pr.setTitle("Server Cluster Hardware");
        pr.setTotalAmount(new BigDecimal("75000.00"));
        pr.setStatus(RequestStatus.SUBMITTED);
        pr.setDepartment(department);
        pr.setRequester(requester);

        PurchaseRequestItem item = new PurchaseRequestItem("High-Density Server Chassis", new BigDecimal("5"), new BigDecimal("15000.00"));
        pr.setLineItems(List.of(item));

        when(requestRepository.findById(102L)).thenReturn(Optional.of(pr));
        when(requestRepository.findByDepartmentId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pr)));
        when(geminiClient.generateText(anyString())).thenReturn(Optional.of(
                "Executive AI Analysis: This server hardware acquisition requires senior review due to substantial budget commitment."
        ));

        AiRiskAssessmentResponse resp = aiService.analyzePurchaseRequest(102L);

        assertNotNull(resp);
        assertEquals(RiskLevel.HIGH, resp.getRiskLevel());
        assertEquals("REQUIRES_SENIOR_AUDIT_AND_EXECUTIVE_APPROVAL", resp.getRecommendation());
        assertTrue(resp.isAiGenerated());
        assertTrue(resp.getDetectedFlags().stream().anyMatch(f -> f.contains("CRITICAL_SPEND_THRESHOLD")));
        assertTrue(resp.getDetectedFlags().stream().anyMatch(f -> f.contains("HIGH_UNIT_PRICE")));
        assertTrue(resp.getAnalysisSummary().contains("substantial budget commitment"));
    }

    @Test
    @DisplayName("Should detect duplicate requisition attempt in same department")
    void testAnalyzeDuplicateRequisitionFlag() {
        PurchaseRequest prCurrent = new PurchaseRequest();
        prCurrent.setId(103L);
        prCurrent.setRequestNumber("PR-20261014-0103");
        prCurrent.setTitle("Identical Laptops Batch");
        prCurrent.setTotalAmount(new BigDecimal("12000.00"));
        prCurrent.setStatus(RequestStatus.DRAFT);
        prCurrent.setDepartment(department);
        prCurrent.setRequester(requester);

        PurchaseRequest prExisting = new PurchaseRequest();
        prExisting.setId(99L);
        prExisting.setRequestNumber("PR-20261014-0099");
        prExisting.setTitle("Identical Laptops Batch");
        prExisting.setTotalAmount(new BigDecimal("12000.00"));
        prExisting.setDepartment(department);

        when(requestRepository.findById(103L)).thenReturn(Optional.of(prCurrent));
        when(requestRepository.findByDepartmentId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(prCurrent, prExisting)));
        when(geminiClient.generateText(anyString())).thenReturn(Optional.empty());

        AiRiskAssessmentResponse resp = aiService.analyzePurchaseRequest(103L);

        assertNotNull(resp);
        assertTrue(resp.getDetectedFlags().stream().anyMatch(f -> f.contains("POTENTIAL_DUPLICATE_ORDER")));
    }

    @Test
    @DisplayName("Should analyze Purchase Order and store risk scoring")
    void testAnalyzePurchaseOrder() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(201L);
        po.setPoNumber("PO-20261014-0201");
        po.setVendorName("Global Tech Supply");
        po.setTotalAmount(new BigDecimal("15000.00"));
        po.setStatus(POStatus.DRAFT);

        when(orderRepository.findById(201L)).thenReturn(Optional.of(po));
        when(geminiClient.generateText(anyString())).thenReturn(Optional.of("PO analysis completed cleanly."));

        AiRiskAssessmentResponse resp = aiService.analyzePurchaseOrder(201L);

        assertNotNull(resp);
        assertEquals(201L, resp.getOrderId());
        assertEquals("PO-20261014-0201", resp.getPoNumber());
        assertEquals(RiskLevel.MEDIUM, resp.getRiskLevel());
        assertTrue(resp.isAiGenerated());
        assertEquals("PO analysis completed cleanly.", resp.getAnalysisSummary());
        verify(orderRepository).save(po);
    }
}
