import { LeaveType } from './leave-status.model';

export interface LeaveRequestCreateRequest {
  employeeId: number | null;
  type: LeaveType;
  startDate: string;
  endDate: string;
  reason: string | null;
}
