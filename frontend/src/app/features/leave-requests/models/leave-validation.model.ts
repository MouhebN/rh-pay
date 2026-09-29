export interface LeaveValidation {
  valid: boolean;
  blockingErrors: string[];
  warnings: string[];
  requestedDays: number;
  remainingLeaveBalance: number | null;
  affectsPayroll: boolean;
}
