import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Specialty, SpecialtyRequest, SpecialtyOffering, SpecialtyOfferingRequest, PageResponse, OrgInfo } from '../../models/org.model';

@Injectable({ providedIn: 'root' })
export class SpecialtyService {
  private readonly apiUrl = '/api/specialties';
  private readonly offeringsUrl = '/api/specialty-offerings';

  constructor(private http: HttpClient) {}

  getAll(params?: {
    degree?: string;
    eduType?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: 'asc' | 'desc';
  }): Observable<PageResponse<Specialty>> {
    let p = new HttpParams();
    if (params?.degree) p = p.set('degree', params.degree);
    if (params?.eduType) p = p.set('eduType', params.eduType);
    if (params?.page !== undefined) p = p.set('page', params.page);
    if (params?.size !== undefined) p = p.set('size', params.size ?? 20);
    if (params?.sortBy) p = p.set('sort', `${params.sortBy},${params.sortDir ?? 'asc'}`);
    return this.http.get<PageResponse<Specialty>>(this.apiUrl, { params: p });
  }

  getByOrg(
    orgId: number,
    params?: { degree?: string; eduType?: string; page?: number; size?: number; sortBy?: string; sortDir?: 'asc' | 'desc' },
  ): Observable<PageResponse<Specialty>> {
    let p = new HttpParams();
    if (params?.degree) p = p.set('degree', params.degree);
    if (params?.eduType) p = p.set('eduType', params.eduType);
    if (params?.page !== undefined) p = p.set('page', params.page);
    if (params?.size !== undefined) p = p.set('size', params.size ?? 100);
    if (params?.sortBy) p = p.set('sort', `${params.sortBy},${params.sortDir ?? 'asc'}`);
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

  getOrgInfo(specialtyId: number): Observable<OrgInfo> {
    return this.http.get<OrgInfo>(`${this.apiUrl}/${specialtyId}/org-info`);
  }

  getOfferings(specialtyId: number): Observable<SpecialtyOffering[]> {
    return this.http.get<SpecialtyOffering[]>(this.offeringsUrl, {
      params: new HttpParams().set('specialtyId', specialtyId),
    });
  }

  getOfferingByExternalId(externalId: number): Observable<SpecialtyOffering | null> {
    return this.http
      .get<SpecialtyOffering>(`${this.offeringsUrl}/by-external-id`, {
        params: new HttpParams().set('externalId', externalId),
      })
      .pipe(catchError(() => of(null)));
  }

  getOfferingById(id: number): Observable<SpecialtyOffering> {
    return this.http.get<SpecialtyOffering>(`${this.offeringsUrl}/${id}`);
  }

  createOffering(request: SpecialtyOfferingRequest): Observable<SpecialtyOffering> {
    return this.http.post<SpecialtyOffering>(this.offeringsUrl, request);
  }

  deleteOffering(id: number): Observable<void> {
    return this.http.delete<void>(`${this.offeringsUrl}/${id}`);
  }
}
