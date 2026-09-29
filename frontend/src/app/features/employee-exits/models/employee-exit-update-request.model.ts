import { ExitStatus } from './exit-status.model';
import { ExitType } from './exit-type.model';

export interface EmployeeExitUpdateRequest {
  type: ExitType;
  status: ExitStatus;
  exitDate: string;
  reason: string;
  notes: string | null;
}
