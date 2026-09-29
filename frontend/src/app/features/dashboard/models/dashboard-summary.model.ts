import { ActionIcon } from '../../../shared/components/icon-action-button/icon-action-button.component';
import { AttendanceStatus } from '../../attendance/models/attendance-status.model';
import { ContractType } from '../../contracts/models/contract-type.model';

export interface DashboardAlert {
  type: string;
  title: string;
  message: string;
  count: number;
  severity: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
  route: string;
  actionLabel: string;
}

export interface QuickStat {
  label: string;
  value: string;
  helper: string;
  icon: ActionIcon;
  severity: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'neutral';
  route: string;
}

export interface AdminDashboardSummary {
  activeEmployeesCount: number;
  inactiveEmployeesCount: number;
  newEmployeesThisMonth: number;
  departmentsCount: number;
  jobPositionsCount: number;
  activeContractsCount: number;
  expiringSoonContractsCount: number;
  expiredContractsCount: number;
  contractsMissingDocumentCount: number;
  scheduledExitsCount: number;
  pendingLeaveRequestsCount: number;
  approvedLeaveRequestsThisMonth: number;
  employeesOnLeaveToday: number;
  leaveRequestsToReviewCount: number;
  presentTodayCount: number;
  lateTodayCount: number;
  absentTodayCount: number;
  incompleteAttendanceTodayCount: number;
  totalOvertimeMinutesThisMonth: number;
  draftPayrollVariablesThisMonth: number;
  validatedPayrollVariablesThisMonth: number;
  draftPayrollCalculationsThisMonth: number;
  finalizedPayrollCalculationsThisMonth: number;
  generatedPayslipsThisMonth: number;
  totalNetPayrollThisMonth: number;
  totalGrossPayrollThisMonth: number;
  alerts: DashboardAlert[];
  mainStats: QuickStat[];
}

export interface EmployeeDashboardSummary {
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  currentContractType: ContractType | null;
  currentContractEndDate: string | null;
  leaveBalanceRemaining: number;
  pendingLeaveRequestsCount: number;
  approvedLeaveRequestsThisYear: number;
  todayAttendanceStatus: AttendanceStatus | null;
  todayCheckInTime: string | null;
  todayCheckOutTime: string | null;
  workedHoursToday: number | null;
  latestNetSalary: number | null;
  latestSalaryMonth: number | null;
  latestSalaryYear: number | null;
  latestPayslipAvailable: boolean;
}
