import { ExitStatus } from './exit-status.model';
import { ExitType } from './exit-type.model';

export interface EmployeeExit {
  id: number;
  employeeId: number;
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  type: ExitType;
  status: ExitStatus;
  exitDate: string;
  reason: string;
  notes: string | null;
  accountDisabled: boolean;
  activeContractTerminated: boolean;
  createdByUsername: string | null;
  completedByUsername: string | null;
  completedAt: string | null;
  cancelledByUsername: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeExitFilter {
  employeeId?: number | null;
  departmentId?: number | null;
  jobPositionId?: number | null;
  type?: ExitType | '';
  status?: ExitStatus | '';
  exitDateFrom?: string;
  exitDateTo?: string;
}

export interface EmployeeExitSummary {
  totalExits: number;
  draftCount: number;
  scheduledCount: number;
  completedCount: number;
  cancelledCount: number;
  upcomingCount: number;
  exitsByType: Record<string, number>;
}
