import { Role } from '../../../core/models/role.model';

export interface UserUpdateRequest {
  username: string;
  email: string;
  role: Role;
}
