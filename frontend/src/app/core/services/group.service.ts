import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StudentGroup, StudentGroupMember, StudentGroupRequest } from '../../models/group.model';
import { PageResponse } from '../../models/org.model';

@Injectable({ providedIn: 'root' })
export class GroupService {
  private readonly apiUrl = '/api/groups';

  constructor(private http: HttpClient) {}

  getAll(params?: { name?: string; page?: number; size?: number; specialtyOfferingId?: number; sortBy?: string; sortDir?: 'asc' | 'desc' }): Observable<PageResponse<StudentGroup>> {
    let p = new HttpParams();
    if (params?.name) p = p.set('name', params.name);
    if (params?.page !== undefined) p = p.set('pageNumber', params.page);
    if (params?.size !== undefined) p = p.set('size', params.size);
    if (params?.specialtyOfferingId != null) p = p.set('specialtyOfferingId', params.specialtyOfferingId);
    if (params?.sortBy) p = p.set('sortBy', params.sortBy);
    if (params?.sortDir) p = p.set('sortDir', params.sortDir);
    return this.http.get<PageResponse<StudentGroup>>(this.apiUrl, { params: p });
  }

  getById(id: number): Observable<StudentGroup> {
    return this.http.get<StudentGroup>(`${this.apiUrl}/${id}`);
  }

  create(request: StudentGroupRequest): Observable<StudentGroup> {
    return this.http.post<StudentGroup>(this.apiUrl, request);
  }

  update(id: number, request: StudentGroupRequest): Observable<StudentGroup> {
    return this.http.put<StudentGroup>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getMembers(groupId: number): Observable<StudentGroupMember[]> {
    return this.http.get<StudentGroupMember[]>(`${this.apiUrl}/${groupId}/members`);
  }

  addMember(groupId: number, bookNumberId: number): Observable<StudentGroupMember> {
    return this.http.post<StudentGroupMember>(`${this.apiUrl}/${groupId}/members/${bookNumberId}`, {});
  }

  removeMember(groupId: number, bookNumberId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${groupId}/members/${bookNumberId}`);
  }
}
