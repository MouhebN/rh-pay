export interface AppSettingsUpdateRequest {
  companyName: string;
  companyAddress: string;
  companyEmail: string;
  companyPhone: string;
  workStartTime: string;
  workEndTime: string;
  expectedDailyHours: number;
  monthlyReferenceHours: number;
  defaultAnnualLeaveDays: number;
  allowNegativeLeaveBalance: boolean;
  overtimeMultiplier: number;
  deductLateMinutesFromSalary: boolean;
  currency: string;
}
