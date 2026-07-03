export type Role = 'LANDLORD' | 'TENANT' | 'PROPERTY_MANAGER' | 'ADMIN';

export interface UserSummary {
  id: string;
  email: string;
  fullName: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** Where each role lands after login / when a role guard rejects them. */
export const ROLE_HOME_ROUTE: Record<Role, string> = {
  LANDLORD: '/properties',
  PROPERTY_MANAGER: '/properties',
  TENANT: '/my-lease',
  ADMIN: '/admin/users'
};
