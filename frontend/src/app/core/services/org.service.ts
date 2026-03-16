import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Organization,
  OrganizationRequest,
  OrganizationShort,
  PageResponse,
  OrgType,
} from '../../models/org.model';

@Injectable({ providedIn: 'root' })
export class OrgService {
  private readonly apiUrl = '/api/orgs';

  constructor(private http: HttpClient) {}

  getAll(params?: {
    orgType?: OrgType;
    page?: number;
    size?: number;
  }): Observable<PageResponse<Organization>> {
    let p = new HttpParams();
    if (params?.orgType) p = p.set('orgType', params.orgType);
    if (params?.page !== undefined) p = p.set('page', params.page);
    if (params?.size !== undefined) p = p.set('size', params.size);
    return this.http.get<PageResponse<Organization>>(this.apiUrl, { params: p });
  }

  getAllShort(orgType?: OrgType): Observable<OrganizationShort[]> {
    let p = new HttpParams();
    if (orgType) p = p.set('orgType', orgType);
    return this.http.get<OrganizationShort[]>(`${this.apiUrl}/short`, { params: p });
  }

  getById(id: number): Observable<Organization> {
    return this.http.get<Organization>(`${this.apiUrl}/${id}`);
  }

  create(request: OrganizationRequest): Observable<Organization> {
    return this.http.post<Organization>(this.apiUrl, request);
  }

  update(id: number, request: OrganizationRequest): Observable<Organization> {
    return this.http.put<Organization>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getDepartmentsByFaculty(facultyId: number): Observable<OrganizationShort[]> {
    return this.http.get<OrganizationShort[]>(`${this.apiUrl}/${facultyId}/departments`);
  }
}
