import { Role } from '../../../core/models/role.model';

export interface UserCreateRequest {
  username: string;
  email: string;
  password: string;
  role: Role;
  enabled: boolean;
}
