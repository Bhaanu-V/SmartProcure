import { apiClient } from '../api/client';
import type { GeneratePORequest, PaginatedResponse, PurchaseOrder } from '../types';

export const orderService = {
  async getOrders(page: number = 0, size: number = 10): Promise<PaginatedResponse<PurchaseOrder>> {
    const response = await apiClient.get<PaginatedResponse<PurchaseOrder>>('/api/orders', {
      params: { page, size },
    });
    return response.data;
  },

  async getOrderById(id: number): Promise<PurchaseOrder> {
    const response = await apiClient.get<PurchaseOrder>(`/api/orders/${id}`);
    return response.data;
  },

  async generateOrder(data: GeneratePORequest): Promise<PurchaseOrder> {
    const response = await apiClient.post<PurchaseOrder>('/api/orders', data);
    return response.data;
  },

  async updateOrderStatus(id: number, status: string, notes?: string): Promise<PurchaseOrder> {
    const response = await apiClient.put<PurchaseOrder>(`/api/orders/${id}/status`, {
      status,
      notes,
    });
    return response.data;
  },

  async downloadPdfBlob(orderId: number): Promise<Blob> {
    const response = await apiClient.get(`/api/orders/${orderId}/pdf`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
