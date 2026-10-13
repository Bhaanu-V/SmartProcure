package com.smartprocure.dto.approval;

import com.smartprocure.entity.ApprovalAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for submitting an approval or rejection decision.
 */
public class ApprovalDecisionRequest {

    @NotNull(message = "Approval action is required (APPROVED or REJECTED)")
    private ApprovalAction action;

    @Size(max = 1000, message = "Comments must not exceed 1000 characters")
    private String comments;

    public ApprovalDecisionRequest() {
    }

    public ApprovalDecisionRequest(ApprovalAction action, String comments) {
        this.action = action;
        this.comments = comments;
    }

    public ApprovalAction getAction() {
        return action;
    }

    public void setAction(ApprovalAction action) {
        this.action = action;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
