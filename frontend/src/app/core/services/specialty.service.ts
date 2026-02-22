import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Specialty, SpecialtyRequest, PageResponse } from '../../models/org.model';

@Injectable({ providedIn: 'root' })
export class SpecialtyService {
  private readonly apiUrl = '/api/specialties';

  constructor(private http: HttpClient) {}

  getAll(params?: {
    degree?: string;
    eduType?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<Specialty>> {
    let p = new HttpParams();
    if (params?.degree) p = p.set('degree', params.degree);
    if (params?.eduType) p = p.set('eduType', params.eduType);
    if (params?.page !== undefined) p = p.set('page', params.page);
    if (params?.size !== undefined) p = p.set('size', params.size ?? 20);
    return this.http.get<PageResponse<Specialty>>(this.apiUrl, { params: p });
  }

  getByOrg(
    orgId: number,
    params?: { degree?: string; eduType?: string; page?: number },
  ): Observable<PageResponse<Specialty>> {
    let p = new HttpParams();
    if (params?.degree) p = p.set('degree', params.degree);
    if (params?.eduType) p = p.set('eduType', params.eduType);
    if (params?.page !== undefined) p = p.set('page', params.page);
    return this.http.get<PageResponse<Specialty>>(`${this.apiUrl}/organization/${orgId}`, {
      params: p,
    });
  }

  getById(id: number): Observable<Specialty> {
    return this.http.get<Specialty>(`${this.apiUrl}/${id}`);
  }

  create(request: SpecialtyRequest): Observable<Specialty> {
    return this.http.post<Specialty>(this.apiUrl, request);
  }

  update(id: number, request: SpecialtyRequest): Observable<Specialty> {
    return this.http.put<Specialty>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
