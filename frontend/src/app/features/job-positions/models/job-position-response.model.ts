import { DepartmentResponse } from '../../departments/models/department-response.model';

export interface JobPositionResponse {
  id: number;
  title: string;
  description: string | null;
  baseSalary: number | null;
  department: DepartmentResponse | null;
  createdAt: string;
  updatedAt: string;
}
