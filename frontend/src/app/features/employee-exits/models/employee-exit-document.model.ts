export type EmployeeExitDocumentType =
  | 'RESIGNATION_LETTER'
  | 'TERMINATION_LETTER'
  | 'END_OF_CONTRACT_DOCUMENT'
  | 'FINAL_SETTLEMENT'
  | 'WORK_CERTIFICATE'
  | 'OTHER';

export interface EmployeeExitDocument {
  id: number;
  employeeExitId: number;
  type: EmployeeExitDocumentType;
  originalFileName: string;
  contentType: string;
  fileSize: number | null;
  visibleToEmployee: boolean;
  uploadedByUsername: string | null;
  uploadedAt: string;
}
