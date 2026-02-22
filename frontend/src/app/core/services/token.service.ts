import { Injectable, signal } from '@angular/core';
import { User } from '../../models/user.model';

@Injectable({ providedIn: 'root' })
export class TokenService {
  currentUser = signal<User | null>(null);
  isAuthenticated = signal<boolean>(false);
  private token: string | null = null;

  setToken(token: string): void {
    this.token = token;
    this.isAuthenticated.set(true);
  }

  getToken(): string | null {
    return this.token;
  }

  clear(): void {
    this.token = null;
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
  }
}
