import { PayrollVariableStatus } from './payroll-variable-status.model';

export interface PayrollVariable {
  id: number;
  employeeId: number;
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  month: number;
  year: number;
  unpaidAbsenceDays: number;
  paidLeaveDays: number;
  sickLeaveDays: number;
  lateMinutes: number;
  overtimeMinutes: number;
  workedDays: number;
  absentDays: number;
  bonusAmount: number;
  advanceAmount: number;
  deductionAmount: number;
  notes: string | null;
  status: PayrollVariableStatus;
  generatedById: number | null;
  generatedByUsername: string | null;
  generatedAt: string | null;
  validatedById: number | null;
  validatedByUsername: string | null;
  validatedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PayrollVariableFilter {
  month?: number | null;
  year?: number | null;
  employeeId?: number | null;
  departmentId?: number | null;
  jobPositionId?: number | null;
  status?: PayrollVariableStatus | '';
}

export interface PayrollVariableGenerationSummary {
  generatedCount: number;
  skippedValidatedCount: number;
  updatedDraftCount: number;
}
