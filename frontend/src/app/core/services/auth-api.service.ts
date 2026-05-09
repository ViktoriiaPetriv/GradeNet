import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest } from '../../models/auth.model';

export interface SetupStatus {
  setupRequired: boolean;
}

export interface AdminSetupRequest {
  email: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly apiUrl = '/api/auth';

  constructor(private http: HttpClient) {}

  checkSetup(): Observable<SetupStatus> {
    return this.http.get<SetupStatus>(`${this.apiUrl}/setup`);
  }

  setupAdmin(request: AdminSetupRequest): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/setup`, request);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request, {
      withCredentials: true,
    });
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/refresh`,
      {},
      {
        withCredentials: true,
      },
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/logout`,
      {},
      {
        withCredentials: true,
      },
    );
  }
}