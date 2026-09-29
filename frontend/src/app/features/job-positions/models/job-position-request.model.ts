export interface JobPositionRequest {
  title: string;
  description: string | null;
  baseSalary: number | null;
  departmentId: number | null;
}
