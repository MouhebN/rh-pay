import { PayslipStatus } from './payslip-status.model';

export interface Payslip {
  id: number;
  employeeId: number;
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  payrollCalculationId: number;
  month: number;
  year: number;
  fileName: string;
  contentType: string;
  fileSize: number | null;
  baseSalary: number;
  grossSalary: number;
  totalDeductions: number;
  netSalary: number;
  status: PayslipStatus;
  generatedByUsername: string | null;
  generatedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface PayslipFilter {
  month?: number | null;
  year?: number | null;
  employeeId?: number | null;
  departmentId?: number | null;
  jobPositionId?: number | null;
  status?: PayslipStatus | '';
}

export interface PayslipGenerationSummary {
  generatedCount: number;
  regeneratedCount: number;
  skippedCount: number;
}
