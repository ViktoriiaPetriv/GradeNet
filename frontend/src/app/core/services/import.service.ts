import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ImportResult,
  ParsedReportMeta,
  DisciplineCheckResult,
  StudentCheckResult,
  CreatedDisciplineInfo,
  GradeComparisonResult,
} from '../../models/import.model';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly base = '/api/import';

  constructor(private http: HttpClient) {}

  parse(file: File): Observable<ParsedReportMeta> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<ParsedReportMeta>(`${this.base}/parse`, fd);
  }

  checkDisciplines(file: File, specialtyId?: number): Observable<DisciplineCheckResult> {
    const fd = new FormData();
    fd.append('file', file);
    const params = specialtyId != null ? `?specialtyId=${specialtyId}` : '';
    return this.http.post<DisciplineCheckResult>(`${this.base}/check-disciplines${params}`, fd);
  }

  createDisciplines(
    file: File,
    disciplineIndices: number[],
    specialtyOfferingId: number,
    academicYear: string
  ): Observable<CreatedDisciplineInfo[]> {
    const fd = new FormData();
    fd.append('file', file);
    const params = new URLSearchParams({
      disciplineIndices: JSON.stringify(disciplineIndices),
      specialtyOfferingId: String(specialtyOfferingId),
      academicYear: academicYear,
    });
    return this.http.post<CreatedDisciplineInfo[]>(
      `${this.base}/create-disciplines?${params}`,
      fd
    );
  }

  checkStudents(file: File): Observable<StudentCheckResult> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<StudentCheckResult>(`${this.base}/check-students`, fd);
  }

  importGradeReport(
    file: File,
    professorMap: Record<number, number>,
    selectedStudentBookNumberIds?: number[],
    overwrite = false
  ): Observable<ImportResult> {
    const fd = new FormData();
    fd.append('file', file);
    const params = new URLSearchParams({ professorMap: JSON.stringify(professorMap) });
    if (selectedStudentBookNumberIds && selectedStudentBookNumberIds.length > 0) {
      params.append('selectedStudentBookNumberIds', JSON.stringify(selectedStudentBookNumberIds));
    }
    if (overwrite) {
      params.append('overwrite', 'true');
    }
    return this.http.post<ImportResult>(`${this.base}/grade-report?${params}`, fd);
  }

  compareGrades(
    file: File,
    professorMap: Record<number, number>,
    selectedStudentBookNumberIds?: number[]
  ): Observable<GradeComparisonResult> {
    const fd = new FormData();
    fd.append('file', file);
    const params = new URLSearchParams({ professorMap: JSON.stringify(professorMap) });
    if (selectedStudentBookNumberIds && selectedStudentBookNumberIds.length > 0) {
      params.append('selectedStudentBookNumberIds', JSON.stringify(selectedStudentBookNumberIds));
    }
    return this.http.post<GradeComparisonResult>(`${this.base}/compare-grades?${params}`, fd);
  }
}
