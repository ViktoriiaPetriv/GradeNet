import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  GradeDTO,
  GradeCreateRequest,
  GradeUpdateRequest,
  GradeBookEntryDTO,
  GradeBookEntryFilter,
  GradeBookEntryCreateRequest,
  BulkGradeEntryDTO,
  BulkGradeCreateRequest,
} from '../../models/grade.model';

@Injectable({ providedIn: 'root' })
export class GradeService {
  private readonly recordsUrl = '/api/records';
  private readonly gradesUrl = '/api/grades';

  constructor(private http: HttpClient) {}

  getEntries(filter: GradeBookEntryFilter = {}): Observable<GradeBookEntryDTO[]> {
    let params = new HttpParams();
    if (filter.bookNumberId != null) params = params.set('bookNumberId', filter.bookNumberId);
    if (filter.specialtyDisciplineId != null) params = params.set('specialtyDisciplineId', filter.specialtyDisciplineId);
    if (filter.professorId != null) params = params.set('professorId', filter.professorId);
    if (filter.academicYear) params = params.set('academicYear', filter.academicYear);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.result) params = params.set('result', filter.result);
    return this.http.get<GradeBookEntryDTO[]>(this.recordsUrl, { params });
  }

  getEntryById(id: number): Observable<GradeBookEntryDTO> {
    return this.http.get<GradeBookEntryDTO>(`${this.recordsUrl}/${id}`);
  }

  getGradesByEntry(entryId: number): Observable<GradeDTO[]> {
    return this.http.get<GradeDTO[]>(this.gradesUrl, { params: { entryId } });
  }

  createGrade(req: GradeCreateRequest): Observable<GradeDTO> {
    return this.http.post<GradeDTO>(this.gradesUrl, req);
  }

  updateGrade(id: number, req: GradeUpdateRequest): Observable<GradeDTO> {
    return this.http.put<GradeDTO>(`${this.gradesUrl}/${id}`, req);
  }

  deleteGrade(id: number): Observable<void> {
    return this.http.delete<void>(`${this.gradesUrl}/${id}`);
  }

  createEntries(req: GradeBookEntryCreateRequest): Observable<GradeBookEntryDTO[]> {
    return this.http.post<GradeBookEntryDTO[]>(this.recordsUrl, req);
  }

  closeEntries(entryIds: number[]): Observable<unknown> {
    return this.http.patch(`${this.recordsUrl}/close`, { entryIds });
  }

  getBulkEntries(specialtyDisciplineId: number, academicYear: string): Observable<BulkGradeEntryDTO[]> {
    const params = new HttpParams()
      .set('specialtyDisciplineId', specialtyDisciplineId)
      .set('academicYear', academicYear);
    return this.http.get<BulkGradeEntryDTO[]>(`${this.recordsUrl}/bulk-view`, { params });
  }

  createBulkGrades(req: BulkGradeCreateRequest): Observable<GradeDTO[]> {
    return this.http.post<GradeDTO[]>(`${this.gradesUrl}/bulk`, req);
  }

  getGroupReport(specialtyDisciplineId: number, academicYear: string): Observable<BulkGradeEntryDTO[]> {
    const params = new HttpParams()
      .set('specialtyDisciplineId', specialtyDisciplineId)
      .set('academicYear', academicYear);
    return this.http.get<BulkGradeEntryDTO[]>(`${this.recordsUrl}/group-report`, { params });
  }
}
