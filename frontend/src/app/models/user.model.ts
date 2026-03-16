import { Role } from './role.enum';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  patronymic?: string;
  email: string;
  birthDate: string;
  role: Role;
  orgId?: number;
}

export interface UserRequest {
  firstName?: string;
  lastName?: string;
  patronymic?: string;
  email: string;
  password?: string;
  birthDate?: string;
  role: Role;
  orgId?: number;
}

export interface StudentInfo {
  bookId: number;
  bookNumber: string;
  bookNumberStatus: string;
  startDate: string;
  endDate?: string;
  specialtyId: number;
  orgId: number;
}

export interface UserProfile {
  id: number;
  firstName?: string;
  lastName?: string;
  patronymic?: string;
  email: string;
  birthDate?: string;
  role: string;
  books: StudentInfo[];
}

export interface ChangePasswordRequest {
  newPassword: string;
}
