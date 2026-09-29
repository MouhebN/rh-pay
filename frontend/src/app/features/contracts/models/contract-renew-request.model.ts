export interface ContractRenewRequest {
  newStartDate: string;
  newEndDate: string | null;
  trialPeriodEndDate: string | null;
  salaryAtSignature: number;
  notes: string | null;
}
