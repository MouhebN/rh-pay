export interface PayrollExportPreviewRow {
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  baseSalary: number;
  grossSalary: number;
  totalDeductions: number;
  netSalary: number;
}

export interface PayrollExportPreview {
  month: number;
  year: number;
  rows: PayrollExportPreviewRow[];
}
