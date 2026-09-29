import { PayrollCalculationStatus } from './payroll-calculation-status.model';

export interface PayrollCalculation {
  id: number;
  employeeId: number;
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  month: number;
  year: number;
  baseSalary: number;
  bonusAmount: number;
  overtimeAmount: number;
  totalDeductions: number;
  grossSalary: number;
  netSalary: number;
  status: PayrollCalculationStatus;
}

export interface PayrollCalculationDetails extends PayrollCalculation {
  payrollVariableId: number;
  dailyRate: number;
  hourlyRate: number;
  unpaidAbsenceDays: number;
  lateMinutes: number;
  overtimeMinutes: number;
  advanceAmount: number;
  deductionAmount: number;
  unpaidAbsenceDeduction: number;
  lateDeduction: number;
  calculatedByUsername: string | null;
  calculatedAt: string | null;
  finalizedByUsername: string | null;
  finalizedAt: string | null;
}

export interface PayrollCalculationFilter {
  month?: number | null;
  year?: number | null;
  employeeId?: number | null;
  departmentId?: number | null;
  jobPositionId?: number | null;
  status?: PayrollCalculationStatus | '';
}

export interface PayrollCalculationGenerationSummary {
  generatedCount: number;
  updatedDraftCount: number;
  skippedFinalizedCount: number;
  skippedInvalidVariablesCount: number;
}
