import { LeaveStatus, LeaveType } from './leave-status.model';

export interface LeaveRequest {
  id: number;
  employeeId: number;
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  type: LeaveType;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason: string | null;
  status: LeaveStatus;
  reviewedById: number | null;
  reviewedByUsername: string | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  hasDocument: boolean;
  documentFileName: string | null;
  documentContentType: string | null;
  documentSize: number | null;
  documentUploadedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminLeaveSummary {
  pendingCount: number;
  approvedThisMonth: number;
  rejectedThisMonth: number;
  cancelledThisMonth: number;
}

export interface EmployeeLeaveSummary {
  pendingRequests: number;
  approvedThisYear: number;
  rejectedThisYear: number;
}

export interface LeaveRequestFilter {
  status?: LeaveStatus | '';
  type?: LeaveType | '';
  employeeId?: number | null;
  departmentId?: number | null;
  startDate?: string;
  endDate?: string;
}
