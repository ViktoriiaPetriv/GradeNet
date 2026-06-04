import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  JournalDisciplineDetail,
  JournalDisciplineStatus,
  JournalImportRequest,
  JournalImportResult,
  JournalSpecialtyDTO,
  JournalStudentStatus,
} from '../../models/journal.model';

@Injectable({ providedIn: 'root' })
export class JournalService {
  private readonly base = '/api/journal';

  constructor(private http: HttpClient) {}

  getSpecialties(filters: {
    degree?: string;
    graduationYear?: number;
    studyForm?: string;
    code?: string;
  }): Observable<JournalSpecialtyDTO[]> {
    let params = new HttpParams();
    if (filters.degree) params = params.set('degree', filters.degree);
    if (filters.graduationYear) params = params.set('graduationYear', filters.graduationYear);
    if (filters.studyForm) params = params.set('studyForm', filters.studyForm);
    if (filters.code) params = params.set('code', filters.code);
    return this.http.get<JournalSpecialtyDTO[]>(`${this.base}/specialties`, { params });
  }

  getStudentsWithStatus(specialtyId: number): Observable<JournalStudentStatus[]> {
    return this.http.get<JournalStudentStatus[]>(`${this.base}/students-status/${specialtyId}`);
  }

  getDisciplinesWithStatus(specialtyId: number): Observable<JournalDisciplineStatus[]> {
    return this.http.get<JournalDisciplineStatus[]>(`${this.base}/disciplines-status/${specialtyId}`);
  }

  getDisciplineDetail(disciplineId: number): Observable<JournalDisciplineDetail> {
    return this.http.get<JournalDisciplineDetail>(`${this.base}/discipline/${disciplineId}`);
  }

  importFromJournal(request: JournalImportRequest): Observable<JournalImportResult> {
    return this.http.post<JournalImportResult>(`${this.base}/import`, request);
  }
}
