import { ContractStatus } from './contract-status.model';
import { ContractType } from './contract-type.model';

export interface Contract {
  id: number;
  employeeId: number;
  employeeFullName: string;
  departmentName: string | null;
  jobPositionName: string | null;
  type: ContractType;
  status: ContractStatus;
  startDate: string;
  endDate: string | null;
  trialPeriodEndDate: string | null;
  salaryAtSignature: number;
  contractReference: string | null;
  notes: string | null;
  documentFileName: string | null;
  documentContentType: string | null;
  documentSize: number | null;
  documentUploadedAt: string | null;
  createdByUsername: string | null;
  updatedByUsername: string | null;
  terminatedByUsername: string | null;
  terminationDate: string | null;
  terminationReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ContractFilter {
  employeeId?: number | null;
  departmentId?: number | null;
  jobPositionId?: number | null;
  type?: ContractType | '';
  status?: ContractStatus | '';
  startDateFrom?: string;
  startDateTo?: string;
  endDateFrom?: string;
  endDateTo?: string;
  expiringSoonDays?: number | null;
}

export interface ContractSummary {
  totalContracts: number;
  activeContracts: number;
  draftContracts: number;
  expiredContracts: number;
  terminatedContracts: number;
  expiringSoonCount: number;
  contractsByType: Record<string, number>;
}

export interface ContractMonitoring {
  activeContractsCount: number;
  expiringSoonCount: number;
  expiredContractsCount: number;
  missingDocumentCount: number;
  actionRequiredCount: number;
}
