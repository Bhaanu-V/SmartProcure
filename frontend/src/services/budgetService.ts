import { apiClient } from '../api/client';
import type { CatalogItem, Department, DepartmentBudget, Supplier } from '../types';

export const budgetService = {
  async getBudgets(year: number = new Date().getFullYear()): Promise<DepartmentBudget[]> {
    const response = await apiClient.get<DepartmentBudget[]>('/api/budgets', {
      params: { year },
    });
    return response.data;
  },

  async getBudgetByDepartment(departmentId: number, year: number = new Date().getFullYear()): Promise<DepartmentBudget> {
    const response = await apiClient.get<DepartmentBudget>(`/api/budgets/department/${departmentId}`, {
      params: { year },
    });
    return response.data;
  },

  async getDepartments(): Promise<Department[]> {
    const response = await apiClient.get<Department[]>('/api/departments');
    return response.data;
  },

  async getSuppliers(): Promise<Supplier[]> {
    const response = await apiClient.get<Supplier[]>('/api/suppliers');
    return response.data;
  },

  async getCatalogItems(): Promise<CatalogItem[]> {
    const response = await apiClient.get<CatalogItem[]>('/api/catalog');
    return response.data;
  },
};
