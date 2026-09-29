import { Role } from '../../../core/models/role.model';

export interface LoginResponse {
  token: string;
  refreshToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  userId: number;
  username: string;
  email: string;
  role: Role;
}
