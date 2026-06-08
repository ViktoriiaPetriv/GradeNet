import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Commission, CommissionMember, CommissionMemberRequest, CommissionRequest } from '../../models/commission.model';

@Injectable({ providedIn: 'root' })
export class CommissionService {
  private readonly apiUrl = '/api/commissions';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Commission[]> {
    return this.http.get<Commission[]>(this.apiUrl);
  }

  getById(id: number): Observable<Commission> {
    return this.http.get<Commission>(`${this.apiUrl}/${id}`);
  }

  create(request: CommissionRequest): Observable<Commission> {
    return this.http.post<Commission>(this.apiUrl, request);
  }

  update(id: number, request: CommissionRequest): Observable<Commission> {
    return this.http.put<Commission>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  addMember(commissionId: number, request: CommissionMemberRequest): Observable<CommissionMember> {
    return this.http.post<CommissionMember>(`${this.apiUrl}/${commissionId}/members`, request);
  }

  removeMember(commissionId: number, memberId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${commissionId}/members/${memberId}`);
  }
}
