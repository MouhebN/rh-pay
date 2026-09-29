import { Role } from '../../../core/models/role.model';

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  role: Role;
  enabled: boolean;
  accountLocked: boolean;
  failedLoginAttempts: number;
  lastLoginAt: string | null;
  passwordChangedAt: string | null;
  createdAt: string;
  updatedAt: string;
}
