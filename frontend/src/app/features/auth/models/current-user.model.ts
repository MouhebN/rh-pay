import { Role } from '../../../core/models/role.model';

export interface CurrentUser {
  id: number;
  username: string;
  email: string;
  role: Role;
}
