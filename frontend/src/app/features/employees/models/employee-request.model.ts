import { ContractType } from '../../contracts/models/contract-type.model';
import { EmploymentStatus } from './employment-status.model';
import { PaymentMethod } from './payment-method.model';

export interface EmployeeContractCreateRequest {
  type: ContractType;
  startDate: string;
  endDate: string | null;
  trialPeriodEndDate: string | null;
  salaryAtSignature: number | null;
  notes: string | null;
  activateImmediately: boolean;
}

export interface EmployeeRequest {
  matricule: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  birthDate: string | null;
  hireDate: string;
  departmentId: number | null;
  jobPositionId: number | null;
  baseSalary: number | null;
  rib: string | null;
  bankName: string | null;
  paymentMethod: PaymentMethod | null;
  userAccountId: number | null;
  status: EmploymentStatus;
  createContract?: boolean;
  contract?: EmployeeContractCreateRequest | null;
}
