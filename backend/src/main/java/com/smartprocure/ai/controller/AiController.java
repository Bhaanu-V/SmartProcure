package com.smartprocure.ai.controller;

import com.smartprocure.ai.config.GeminiProperties;
import com.smartprocure.ai.dto.AiRiskAssessmentResponse;
import com.smartprocure.ai.dto.AiStatusResponse;
import com.smartprocure.ai.service.ProcurementAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI-assisted procurement risk assessment and spend anomaly detection.
 * Enforces strict human-in-the-loop safeguards: AI recommendations are strictly advisory
 * and cannot approve purchase requests, override department budgets, or bypass security checks.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ProcurementAiService aiService;
    private final GeminiProperties geminiProperties;

    public AiController(ProcurementAiService aiService, GeminiProperties geminiProperties) {
        this.aiService = aiService;
        this.geminiProperties = geminiProperties;
    }

    @GetMapping("/status")
    public ResponseEntity<AiStatusResponse> getStatus() {
        boolean configured = geminiProperties.isConfigured();
        String status = configured ? "ACTIVE_GENAI" : "DETERMINISTIC_HEURISTIC_FALLBACK";
        String policy = "AI recommendations are strictly advisory. Human authorization is mandatory for all procurement and approval decisions.";

        AiStatusResponse resp = new AiStatusResponse(configured, geminiProperties.getModel(), status, policy);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/requests/{id}/analyze")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'FINANCE', 'PROCUREMENT', 'ADMIN')")
    public ResponseEntity<AiRiskAssessmentResponse> analyzePurchaseRequest(@PathVariable("id") Long requestId) {
        AiRiskAssessmentResponse assessment = aiService.analyzePurchaseRequest(requestId);
        return ResponseEntity.ok(assessment);
    }

    @PostMapping("/orders/{id}/analyze")
    @PreAuthorize("hasAnyRole('MANAGER', 'FINANCE', 'PROCUREMENT', 'ADMIN')")
    public ResponseEntity<AiRiskAssessmentResponse> analyzePurchaseOrder(@PathVariable("id") Long orderId) {
        AiRiskAssessmentResponse assessment = aiService.analyzePurchaseOrder(orderId);
        return ResponseEntity.ok(assessment);
    }
}
