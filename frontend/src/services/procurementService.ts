import { apiClient } from '../api/client';
import type { CreatePurchaseRequest, PaginatedResponse, PurchaseRequest } from '../types';

export const procurementService = {
  async getRequests(page: number = 0, size: number = 10): Promise<PaginatedResponse<PurchaseRequest>> {
    const response = await apiClient.get<PaginatedResponse<PurchaseRequest>>('/api/requests', {
      params: { page, size },
    });
    return response.data;
  },

  async getRequestById(id: number): Promise<PurchaseRequest> {
    const response = await apiClient.get<PurchaseRequest>(`/api/requests/${id}`);
    return response.data;
  },

  async createDraft(data: CreatePurchaseRequest): Promise<PurchaseRequest> {
    const response = await apiClient.post<PurchaseRequest>('/api/requests', data);
    return response.data;
  },

  async updateDraft(id: number, data: CreatePurchaseRequest): Promise<PurchaseRequest> {
    const response = await apiClient.put<PurchaseRequest>(`/api/requests/${id}`, data);
    return response.data;
  },

  async submitRequest(id: number): Promise<PurchaseRequest> {
    const response = await apiClient.post<PurchaseRequest>(`/api/requests/${id}/submit`);
    return response.data;
  },

  async cancelRequest(id: number, reason: string): Promise<PurchaseRequest> {
    const response = await apiClient.post<PurchaseRequest>(`/api/requests/${id}/cancel`, null, {
      params: { reason },
    });
    return response.data;
  },
};
