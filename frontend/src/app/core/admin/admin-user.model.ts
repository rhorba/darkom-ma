import { Role } from '../auth/auth.model';

export interface AdminUser {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  active: boolean;
}
