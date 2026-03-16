import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap, ReplaySubject } from 'rxjs';
import { AuthResponse, LoginRequest } from '../../models/auth.model';
import { TokenService } from './token.service';
import { AuthApiService } from './auth-api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _sessionReady$ = new ReplaySubject<boolean>(1);
  readonly sessionReady$ = this._sessionReady$.asObservable();

  private router = inject(Router);
  private tokenService = inject(TokenService);
  private authApi = inject(AuthApiService);

  constructor() {
    this.tryRestoreSession();
  }

  private tryRestoreSession(): void {
    this.authApi.refresh().subscribe({
      next: (res) => {
        this.setSession(res);
        this._sessionReady$.next(true);
      },
      error: (err) => {
        this.tokenService.clear();
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
      next: () => this.finalizeLogout(),
      error: () => this.finalizeLogout(),
    });
  }

  private finalizeLogout(): void {
    this.tokenService.clear();
    this._sessionReady$.next(false);
    this.router.navigate(['/login']);
  }

  private setSession(res: AuthResponse): void {
    this.tokenService.setToken(res.token);
    this.tokenService.currentUser.set(res.user);
    this.tokenService.isAuthenticated.set(true);
  }

  getRole(): string | null {
    return this.tokenService.currentUser()?.role ?? null;
  }

  hasRole(role: string): boolean {
    return this.tokenService.currentUser()?.role === role;
  }
}
