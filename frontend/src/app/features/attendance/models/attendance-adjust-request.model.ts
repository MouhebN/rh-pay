import { AttendanceStatus } from './attendance-status.model';

export interface AttendanceAdjustRequest {
  workDate: string;
  checkInTime: string | null;
  checkOutTime: string | null;
  status: AttendanceStatus;
  adminNote: string | null;
}
