import { apiClient } from '../api/client';
import type { AiRiskAssessmentResponse, AiStatusResponse } from '../types';

export const aiService = {
  async getStatus(): Promise<AiStatusResponse> {
    const response = await apiClient.get<AiStatusResponse>('/api/ai/status');
    return response.data;
  },

  async analyzeRequest(requestId: number): Promise<AiRiskAssessmentResponse> {
    const response = await apiClient.post<AiRiskAssessmentResponse>(`/api/ai/requests/${requestId}/analyze`);
    return response.data;
  },

  async analyzeOrder(orderId: number): Promise<AiRiskAssessmentResponse> {
    const response = await apiClient.post<AiRiskAssessmentResponse>(`/api/ai/orders/${orderId}/analyze`);
    return response.data;
  },
};
