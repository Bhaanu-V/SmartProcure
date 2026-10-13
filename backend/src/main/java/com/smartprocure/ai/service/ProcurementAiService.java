package com.smartprocure.ai.service;

import com.smartprocure.ai.client.GeminiClient;
import com.smartprocure.ai.dto.AiRiskAssessmentResponse;
import com.smartprocure.ai.dto.RiskLevel;
import com.smartprocure.audit.Auditable;
import com.smartprocure.entity.PurchaseOrder;
import com.smartprocure.entity.PurchaseRequest;
import com.smartprocure.entity.PurchaseRequestItem;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.PurchaseOrderRepository;
import com.smartprocure.repository.PurchaseRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service providing rule-based anomaly detection fused with Google Gemini Generative AI
 * for procurement risk evaluation and executive spend summaries.
 */
@Service
public class ProcurementAiService {

    private static final Logger log = LoggerFactory.getLogger(ProcurementAiService.class);

    private final GeminiClient geminiClient;
    private final PurchaseRequestRepository requestRepository;
    private final PurchaseOrderRepository orderRepository;

    public ProcurementAiService(GeminiClient geminiClient,
                                PurchaseRequestRepository requestRepository,
                                PurchaseOrderRepository orderRepository) {
        this.geminiClient = geminiClient;
        this.requestRepository = requestRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Performs anomaly detection and AI risk evaluation for a Purchase Request.
     */
    @Auditable(action = "AI_ANALYZE_REQUEST", entityName = "PurchaseRequest")
    @Transactional
    public AiRiskAssessmentResponse analyzePurchaseRequest(Long requestId) {
        PurchaseRequest pr = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", "id", requestId));

        List<String> flags = new ArrayList<>();
        BigDecimal baseScore = calculateHeuristicRisk(pr.getTotalAmount(), pr.getLineItems(), flags);

        // Check for potential duplicate requests in the same department
        if (pr.getDepartment() != null) {
            long duplicateCount = requestRepository.findByDepartmentId(pr.getDepartment().getId(), org.springframework.data.domain.Pageable.unpaged())
                    .stream()
                    .filter(other -> !other.getId().equals(pr.getId()) &&
                            other.getTitle().equalsIgnoreCase(pr.getTitle()) &&
                            other.getTotalAmount().compareTo(pr.getTotalAmount()) == 0)
                    .count();
            if (duplicateCount > 0) {
                flags.add("POTENTIAL_DUPLICATE_ORDER: Matching requisition title and identical total amount detected.");
                baseScore = baseScore.add(new BigDecimal("25.00"));
            }
        }

        baseScore = baseScore.min(new BigDecimal("100.00"));
        RiskLevel level = determineRiskLevel(baseScore);

        String prompt = buildPromptForRequisition(pr, flags, baseScore, level);
        Optional<String> geminiOutput = geminiClient.generateText(prompt);

        String summary;
        String recommendation;
        boolean isAiGenerated;

        if (geminiOutput.isPresent()) {
            summary = cleanGeminiOutput(geminiOutput.get());
            recommendation = deriveRecommendation(level);
            isAiGenerated = true;
        } else {
            summary = generateDeterministicSummary(pr.getRequestNumber(), pr.getTotalAmount(), flags, level);
            recommendation = deriveRecommendation(level);
            isAiGenerated = false;
        }

        pr.setAiRiskScore(baseScore.setScale(2, RoundingMode.HALF_UP));
        pr.setAiRecommendation(recommendation);
        pr.setAiAnalysisSummary(summary);
        requestRepository.save(pr);

        AiRiskAssessmentResponse resp = new AiRiskAssessmentResponse();
        resp.setRequestId(pr.getId());
        resp.setRequestNumber(pr.getRequestNumber());
        resp.setRiskScore(baseScore.setScale(2, RoundingMode.HALF_UP));
        resp.setRiskLevel(level);
        resp.setRecommendation(recommendation);
        resp.setAnalysisSummary(summary);
        resp.setDetectedFlags(flags);
        resp.setAiGenerated(isAiGenerated);

        return resp;
    }

    /**
     * Performs anomaly detection and AI risk evaluation for a Purchase Order.
     */
    @Auditable(action = "AI_ANALYZE_ORDER", entityName = "PurchaseOrder")
    @Transactional
    public AiRiskAssessmentResponse analyzePurchaseOrder(Long orderId) {
        PurchaseOrder po = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", orderId));

        List<String> flags = new ArrayList<>();
        BigDecimal baseScore = calculateHeuristicRisk(po.getTotalAmount(), null, flags);
        baseScore = baseScore.min(new BigDecimal("100.00"));
        RiskLevel level = determineRiskLevel(baseScore);

        String prompt = String.format("Analyze purchase order %s for vendor '%s' with total $%s. Risk level: %s. Flags: %s. Provide a 2-sentence executive summary.",
                po.getPoNumber(), po.getVendorName(), po.getTotalAmount(), level, flags);
        Optional<String> geminiOutput = geminiClient.generateText(prompt);

        String summary = geminiOutput.map(this::cleanGeminiOutput).orElseGet(() ->
                generateDeterministicSummary(po.getPoNumber(), po.getTotalAmount(), flags, level));
        String recommendation = deriveRecommendation(level);

        po.setAiRiskScore(baseScore.setScale(2, RoundingMode.HALF_UP));
        po.setAiRecommendation(recommendation);
        po.setAiAnalysisSummary(summary);
        orderRepository.save(po);

        AiRiskAssessmentResponse resp = new AiRiskAssessmentResponse();
        resp.setOrderId(po.getId());
        resp.setPoNumber(po.getPoNumber());
        resp.setRiskScore(baseScore.setScale(2, RoundingMode.HALF_UP));
        resp.setRiskLevel(level);
        resp.setRecommendation(recommendation);
        resp.setAnalysisSummary(summary);
        resp.setDetectedFlags(flags);
        resp.setAiGenerated(geminiOutput.isPresent());

        return resp;
    }

    private BigDecimal calculateHeuristicRisk(BigDecimal totalAmount, List<PurchaseRequestItem> lineItems, List<String> flags) {
        BigDecimal score = new BigDecimal("10.00"); // Baseline normal score

        if (totalAmount != null) {
            if (totalAmount.compareTo(new BigDecimal("50000.00")) > 0) {
                flags.add("CRITICAL_SPEND_THRESHOLD: Order exceeds $50,000 enterprise cap.");
                score = score.add(new BigDecimal("40.00"));
            } else if (totalAmount.compareTo(new BigDecimal("10000.00")) > 0) {
                flags.add("HIGH_SPEND_THRESHOLD: Order exceeds $10,000 threshold.");
                score = score.add(new BigDecimal("20.00"));
            }
        }

        if (lineItems != null) {
            for (PurchaseRequestItem item : lineItems) {
                if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(new BigDecimal("3000.00")) > 0) {
                    flags.add("HIGH_UNIT_PRICE: Item '" + item.getItemDescription() + "' exceeds $3,000 per unit.");
                    score = score.add(new BigDecimal("15.00"));
                    break;
                }
            }
            for (PurchaseRequestItem item : lineItems) {
                if (item.getQuantity() != null && item.getQuantity().compareTo(new BigDecimal("100.00")) > 0) {
                    flags.add("BULK_QUANTITY: Item '" + item.getItemDescription() + "' quantity exceeds 100 units.");
                    score = score.add(new BigDecimal("10.00"));
                    break;
                }
            }
        }

        return score;
    }

    private RiskLevel determineRiskLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("60.00")) >= 0) {
            return RiskLevel.HIGH;
        } else if (score.compareTo(new BigDecimal("30.00")) >= 0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String deriveRecommendation(RiskLevel level) {
        switch (level) {
            case HIGH:
                return "REQUIRES_SENIOR_AUDIT_AND_EXECUTIVE_APPROVAL";
            case MEDIUM:
                return "STANDARD_FINANCE_VERIFICATION_RECOMMENDED";
            case LOW:
            default:
                return "ROUTINE_APPROVAL_ELIGIBLE";
        }
    }

    private String buildPromptForRequisition(PurchaseRequest pr, List<String> flags, BigDecimal score, RiskLevel level) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an enterprise procurement risk analysis AI for SmartProcure. ");
        sb.append("Analyze the following purchase requisition and output a concise 2-sentence executive summary.\n");
        sb.append("Requisition #: ").append(pr.getRequestNumber()).append("\n");
        sb.append("Title: ").append(pr.getTitle()).append("\n");
        sb.append("Department: ").append(pr.getDepartment() != null ? pr.getDepartment().getName() : "Unknown").append("\n");
        sb.append("Total Amount: $").append(pr.getTotalAmount()).append(" ").append(pr.getCurrency()).append("\n");
        sb.append("Risk Score: ").append(score).append(" (").append(level).append(")\n");
        sb.append("Detected Anomaly Flags: ").append(String.join("; ", flags)).append("\n");
        sb.append("Provide a professional, objective analysis focusing on budget impact and risk mitigation.");
        return sb.toString();
    }

    private String cleanGeminiOutput(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?m)^```[a-zA-Z]*", "").replaceAll("```$", "").trim();
    }

    private String generateDeterministicSummary(String referenceNumber, BigDecimal amount, List<String> flags, RiskLevel level) {
        if (flags.isEmpty()) {
            return String.format(
                    "Deterministic Analysis: Requisition %s totaling $%s complies with routine procurement thresholds with no anomalous spending patterns detected (Risk: %s).",
                    referenceNumber, amount != null ? amount.toPlainString() : "0.00", level);
        } else {
            return String.format(
                    "Deterministic Analysis: Requisition %s totaling $%s triggered %d compliance flag(s): %s. Scored at %s risk level.",
                    referenceNumber, amount != null ? amount.toPlainString() : "0.00", flags.size(), String.join(", ", flags), level);
        }
    }
}
