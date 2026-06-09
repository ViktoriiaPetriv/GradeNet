import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Commission, CommissionMember, CommissionMemberRequest, CommissionRequest } from '../../models/commission.model';
import { PageResponse } from '../../models/page.model';

@Injectable({ providedIn: 'root' })
export class CommissionService {
  private readonly apiUrl = '/api/commissions';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Commission[]> {
    return this.http.get<Commission[]>(this.apiUrl);
  }

  getPage(page: number, size: number, status: string, sortBy?: string, sortDir?: string): Observable<PageResponse<Commission>> {
    let params = new HttpParams().set('page', page).set('size', size).set('status', status);
    if (sortBy) params = params.set('sortBy', sortBy);
    if (sortDir) params = params.set('sortDir', sortDir);
    return this.http.get<PageResponse<Commission>>(`${this.apiUrl}/paged`, { params });
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
