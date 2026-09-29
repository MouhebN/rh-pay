import { AttendanceSource } from './attendance-source.model';
import { AttendanceStatus } from './attendance-status.model';

export interface AttendanceRecord {
  id: number;
  employeeId: number;
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  workDate: string;
  checkInTime: string | null;
  checkOutTime: string | null;
  workedHours: number | null;
  lateMinutes: number | null;
  overtimeMinutes: number | null;
  status: AttendanceStatus;
  source: AttendanceSource;
  adminNote: string | null;
  adjustedById: number | null;
  adjustedByUsername: string | null;
  adjustedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface TodayAttendance {
  workDate: string;
  checkInTime: string | null;
  checkOutTime: string | null;
  workedHours: number | null;
  lateMinutes: number | null;
  overtimeMinutes: number | null;
  status: AttendanceStatus | null;
  canCheckIn: boolean;
  canCheckOut: boolean;
  onApprovedLeave: boolean;
  message: string;
}

export interface AttendanceFilter {
  employeeId?: number | null;
  departmentId?: number | null;
  jobPositionId?: number | null;
  startDate?: string;
  endDate?: string;
  status?: AttendanceStatus | '';
  source?: AttendanceSource | '';
}
