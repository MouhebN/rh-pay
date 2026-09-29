import { LeaveType } from './leave-status.model';

export interface MonthlyLeaveStat {
  month: number;
  approvedDays: number;
  requestCount: number;
}

export interface LeaveTypeStat {
  type: LeaveType;
  count: number;
  totalDays: number;
}

export interface DepartmentLeaveStat {
  departmentName: string;
  count: number;
  totalDays: number;
}

export interface AdminLeaveStatistics {
  pendingRequestsCount: number;
  approvedRequestsThisMonth: number;
  rejectedRequestsThisMonth: number;
  cancelledRequestsThisMonth: number;
  totalApprovedLeaveDaysThisMonth: number;
  totalUnpaidLeaveDaysThisMonth: number;
  employeesOnLeaveToday: number;
  monthlyStats: MonthlyLeaveStat[];
  leaveTypeStats: LeaveTypeStat[];
  departmentStats: DepartmentLeaveStat[];
}

export interface EmployeeLeaveStatistics {
  myPendingRequestsCount: number;
  myApprovedRequestsThisYear: number;
  myRejectedRequestsThisYear: number;
  myTotalApprovedDaysThisYear: number;
  myPaidLeaveDaysThisYear: number;
  myUnpaidLeaveDaysThisYear: number;
  monthlyStats: MonthlyLeaveStat[];
  leaveTypeStats: LeaveTypeStat[];
}
