import { Role } from '../../../core/models/role.model';
import { DepartmentResponse } from '../../departments/models/department-response.model';
import { JobPositionResponse } from '../../job-positions/models/job-position-response.model';
import { EmploymentStatus } from './employment-status.model';
import { PaymentMethod } from './payment-method.model';

export interface EmployeeResponse {
  id: number;
  matricule: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  birthDate: string | null;
  hireDate: string;
  department: DepartmentResponse | null;
  jobPosition: JobPositionResponse | null;
  baseSalary: number | null;
  rib: string | null;
  bankName: string | null;
  paymentMethod: PaymentMethod | null;
  userAccountId: number | null;
  username: string | null;
  userRole: Role | null;
  status: EmploymentStatus;
  createdAt: string;
  updatedAt: string;
}
