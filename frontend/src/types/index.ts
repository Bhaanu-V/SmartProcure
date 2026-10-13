export type Role = 'ROLE_EMPLOYEE' | 'ROLE_MANAGER' | 'ROLE_FINANCE' | 'ROLE_PROCUREMENT' | 'ROLE_ADMIN' | 'EMPLOYEE' | 'MANAGER' | 'FINANCE' | 'PROCUREMENT' | 'ADMIN';

export type RequestStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'PENDING_MANAGER_APPROVAL'
  | 'PENDING_FINANCE_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'PO_CREATED'
  | 'CANCELLED';

export type POStatus = 'DRAFT' | 'ISSUED' | 'PARTIALLY_FULFILLED' | 'FULFILLED' | 'CANCELLED';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface User {
  id: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: Role;
  departmentId?: number;
  departmentName?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  role: Role;
  departmentId?: number;
  departmentName?: string;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password?: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
  role: Role;
  departmentId: number;
}

export interface Department {
  id: number;
  name: string;
  code: string;
  description?: string;
}

export interface DepartmentBudget {
  id: number;
  departmentId: number;
  departmentName: string;
  fiscalYear: number;
  allocatedAmount: number;
  spentAmount: number;
  reservedAmount: number;
  availableAmount: number;
}

export interface Supplier {
  id: number;
  name: string;
  contactEmail: string;
  contactPhone?: string;
  taxIdentifier?: string;
  paymentTerms?: string;
  status: string;
}

export interface CatalogItem {
  id: number;
  itemCode: string;
  name: string;
  description?: string;
  category: string;
  unitPrice: number;
  supplierId: number;
  supplierName?: string;
}

export interface PurchaseRequestItem {
  id?: number;
  catalogItemId?: number;
  itemDescription: string;
  category?: string;
  quantity: number;
  unitPrice: number;
  totalPrice?: number;
}

export interface PurchaseRequest {
  id: number;
  requestNumber: string;
  title: string;
  justification?: string;
  currency: string;
  totalAmount: number;
  status: RequestStatus;
  requesterId: number;
  requesterName: string;
  departmentId: number;
  departmentName: string;
  lineItems: PurchaseRequestItem[];
  aiRiskScore?: number;
  aiRecommendation?: string;
  aiAnalysisSummary?: string;
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePurchaseRequest {
  title: string;
  justification?: string;
  currency?: string;
  departmentId: number;
  lineItems: {
    catalogItemId?: number;
    itemDescription: string;
    category?: string;
    quantity: number;
    unitPrice: number;
  }[];
}

export interface ApprovalHistoryItem {
  id: number;
  requestId: number;
  stage: 'MANAGER' | 'FINANCE';
  action: 'APPROVED' | 'REJECTED';
  actorUsername: string;
  actorRole: string;
  comments?: string;
  actedAt: string;
}

export interface ApprovalDecisionRequest {
  action: 'APPROVED' | 'REJECTED';
  comments?: string;
}

export interface POLineItem {
  id: number;
  itemDescription: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface PurchaseOrder {
  id: number;
  poNumber: string;
  purchaseRequestId: number;
  requestNumber: string;
  supplierId: number;
  vendorName: string;
  totalAmount: number;
  currency: string;
  status: POStatus;
  issuedAt?: string;
  pdfGenerated: boolean;
  notes?: string;
  departmentName: string;
  requesterUsername: string;
  lineItems: POLineItem[];
  createdAt: string;
  updatedAt: string;
}

export interface GeneratePORequest {
  purchaseRequestId: number;
  supplierId: number;
  notes?: string;
}

export interface AiRiskAssessmentResponse {
  requestId?: number;
  requestNumber?: string;
  orderId?: number;
  poNumber?: string;
  riskScore: number;
  riskLevel: RiskLevel;
  recommendation: string;
  analysisSummary: string;
  detectedFlags: string[];
  aiGenerated: boolean;
  generatedAt: string;
  disclaimer: string;
}

export interface AiStatusResponse {
  configured: boolean;
  model: string;
  serviceStatus: string;
  humanInTheLoopPolicy: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
