import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserRequest, UserProfile, ChangePasswordRequest } from '../../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  findAll(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl);
  }

  findById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  create(request: UserRequest): Observable<User> {
    return this.http.post<User>(this.apiUrl, request);
  }

  update(id: number, request: UserRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getProfile(id: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/${id}/profile`);
  }

  getMyProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/me/profile`);
  }

  changePassword(id: number, request: ChangePasswordRequest): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/password`, request);
  }

  searchStudents(query: string): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/search`, { params: { query } });
  }

  getProfessors(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl, { params: { role: 'PROFESSOR' } });
  }
}
