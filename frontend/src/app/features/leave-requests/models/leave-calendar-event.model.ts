import { LeaveStatus, LeaveType } from './leave-status.model';

export interface LeaveCalendarEvent {
  leaveRequestId: number;
  employeeId: number;
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  type: LeaveType;
  status: LeaveStatus;
  startDate: string;
  endDate: string;
  totalDays: number;
  title: string;
  color: string;
}
