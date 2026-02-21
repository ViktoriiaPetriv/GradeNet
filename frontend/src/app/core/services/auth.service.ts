import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, filter, tap } from 'rxjs';
import { AuthResponse, LoginRequest } from '../../models/auth.model';
import { TokenService } from './token.service';
import { AuthApiService } from './auth-api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _sessionReady$ = new BehaviorSubject<boolean | null>(null);
  readonly sessionReady$ = this._sessionReady$.pipe(filter((v): v is boolean => v !== null));

  private router = inject(Router);
  private tokenService = inject(TokenService);
  private authApi = inject(AuthApiService);

  constructor() {
    this.tryRestoreSession();
  }

  getSessionState(): boolean | null {
    return this._sessionReady$.getValue();
  }

  isAuthenticated(): boolean {
    return this.tokenService.isAuthenticated();
  }

  get currentUser() {
    return this.tokenService.currentUser;
  }

  private tryRestoreSession(): void {
    this.authApi.refresh().subscribe({
      next: (res) => {
        console.log('✅ Session restored');
        this.setSession(res);
        this._sessionReady$.next(true);
      },
      error: (err) => {
        console.log('❌ Restore failed:', err.status);
        this._sessionReady$.next(false);
      },
    });
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.authApi.login(request).pipe(
      tap((res) => {
        this.setSession(res);
        this._sessionReady$.next(true);
      }),
    );
  }

  refresh(): Observable<AuthResponse> {
    return this.authApi.refresh().pipe(tap((res) => this.setSession(res)));
  }

  logout(): void {
    this.authApi.logout().subscribe({
      complete: () => this.clearSession(),
      error: () => this.clearSession(),
    });
  }

  private setSession(res: AuthResponse): void {
    this.tokenService.setToken(res.token);
    this.tokenService.currentUser.set(res.user);
    this.tokenService.isAuthenticated.set(true);
    localStorage.setItem('auth_user', JSON.stringify(res.user));
  }

  private clearSession(): void {
    this.tokenService.clear();
    this._sessionReady$.next(false);
    this.router.navigate(['/login']);
  }

  getRole(): string | null {
    return this.tokenService.currentUser()?.role ?? null;
  }

  hasRole(role: string): boolean {
    return this.tokenService.currentUser()?.role === role;
  }
}
