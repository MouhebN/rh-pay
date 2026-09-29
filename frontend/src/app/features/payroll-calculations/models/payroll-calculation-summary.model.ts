export interface PayrollCalculationSummary {
  totalEmployeesCalculated: number;
  draftCount: number;
  finalizedCount: number;
  cancelledCount: number;
  totalGrossSalary: number;
  totalNetSalary: number;
  totalDeductions: number;
  totalBonuses: number;
  totalAdvances: number;
  totalOvertimeAmount: number;
  totalUnpaidAbsenceDeduction: number;
}
