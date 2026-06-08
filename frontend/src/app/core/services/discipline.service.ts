import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DisciplineDTO, DisciplineCreateRequest, DisciplineUpdateRequest, SpecialtyDisciplineDTO, HoursCreateRequest, HoursDTO } from '../../models/discipline.model';

@Injectable({ providedIn: 'root' })
export class DisciplineService {
  private readonly apiUrl = '/api/disciplines';

  constructor(private http: HttpClient) {}

  getAll(): Observable<DisciplineDTO[]> {
    return this.http.get<DisciplineDTO[]>(this.apiUrl);
  }

  getById(id: number): Observable<DisciplineDTO> {
    return this.http.get<DisciplineDTO>(`${this.apiUrl}/${id}`);
  }

  create(request: DisciplineCreateRequest): Observable<any> {
    return this.http.post(this.apiUrl, request);
  }

  update(id: number, request: DisciplineUpdateRequest): Observable<DisciplineDTO> {
    return this.http.put<DisciplineDTO>(`${this.apiUrl}/${id}`, request);
  }

  getSpecialtyDisciplineById(id: number): Observable<SpecialtyDisciplineDTO> {
    return this.http.get<SpecialtyDisciplineDTO>(`/api/specialty-disciplines/${id}`);
  }

  getSpecialtyDisciplines(disciplineId: number): Observable<SpecialtyDisciplineDTO[]> {
    const params = new HttpParams().set('disciplineId', disciplineId);
    return this.http.get<SpecialtyDisciplineDTO[]>('/api/specialty-disciplines', { params });
  }

  getAllSpecialtyDisciplines(): Observable<SpecialtyDisciplineDTO[]> {
    return this.http.get<SpecialtyDisciplineDTO[]>('/api/specialty-disciplines');
  }

  addSpecialtyDiscipline(specialtyId: number, disciplineId: number): Observable<SpecialtyDisciplineDTO> {
    const params = new HttpParams().set('disciplineId', disciplineId);
    return this.http.post<SpecialtyDisciplineDTO>(`/api/specialty-disciplines/${specialtyId}`, null, { params });
  }

  addHours(specialtyDisciplineId: number, request: HoursCreateRequest): Observable<HoursDTO> {
    const params = new HttpParams().set('specialtyDisciplineId', specialtyDisciplineId);
    return this.http.post<HoursDTO>('/api/hours', request, { params });
  }

  updateHours(id: number, request: HoursCreateRequest): Observable<HoursDTO> {
    return this.http.put<HoursDTO>(`/api/hours/${id}`, request);
  }

  deleteHours(id: number): Observable<void> {
    return this.http.delete<void>(`/api/hours/${id}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
