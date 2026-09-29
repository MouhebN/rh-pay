export interface LeaveBalance {
  id: number;
  employeeId: number;
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  year: number;
  acquiredDays: number;
  consumedDays: number;
  pendingDays: number;
  remainingDays: number;
  createdAt: string;
  updatedAt: string;
}

export interface LeaveBalanceSummary {
  year: number;
  acquiredDays: number;
  consumedDays: number;
  pendingDays: number;
  remainingDays: number;
}

export interface LeaveBalanceUpdateRequest {
  year: number | null;
  acquiredDays: number;
}
