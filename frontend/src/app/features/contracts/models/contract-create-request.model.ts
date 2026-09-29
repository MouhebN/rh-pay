import { ContractStatus } from './contract-status.model';
import { ContractType } from './contract-type.model';

export interface ContractCreateRequest {
  employeeId: number;
  type: ContractType;
  status: ContractStatus | null;
  startDate: string;
  endDate: string | null;
  trialPeriodEndDate: string | null;
  salaryAtSignature: number;
  contractReference: string | null;
  notes: string | null;
}
