import { apiClient } from '../api/client';
import type { ApprovalDecisionRequest, ApprovalHistoryItem, PaginatedResponse, PurchaseRequest } from '../types';

export const approvalService = {
  async getApprovalQueue(page: number = 0, size: number = 10): Promise<PaginatedResponse<PurchaseRequest>> {
    const response = await apiClient.get<PaginatedResponse<PurchaseRequest>>('/api/approvals/queue', {
      params: { page, size },
    });
    return response.data;
  },

  async processDecision(requestId: number, decision: ApprovalDecisionRequest): Promise<PurchaseRequest> {
    const response = await apiClient.post<PurchaseRequest>(`/api/approvals/requests/${requestId}/decision`, decision);
    return response.data;
  },

  async getApprovalHistory(requestId: number): Promise<ApprovalHistoryItem[]> {
    const response = await apiClient.get<ApprovalHistoryItem[]>(`/api/approvals/requests/${requestId}/history`);
    return response.data;
  },
};
