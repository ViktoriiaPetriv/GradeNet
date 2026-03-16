import { User } from './user.model';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface CurrentUser {
  id: number;
  email: string;
  role: string;
  orgId?: number;
}
