import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest } from '../../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly apiUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

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
