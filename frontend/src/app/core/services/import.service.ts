import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ImportResult, ParsedReportMeta } from '../../models/import.model';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly base = '/api/import';

  constructor(private http: HttpClient) {}

  parse(file: File): Observable<ParsedReportMeta> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<ParsedReportMeta>(`${this.base}/parse`, fd);
  }

  importGradeReport(file: File, professorMap: Record<number, number>): Observable<ImportResult> {
    const fd = new FormData();
    fd.append('file', file);
    const params = new URLSearchParams({ professorMap: JSON.stringify(professorMap) });
    return this.http.post<ImportResult>(`${this.base}/grade-report?${params}`, fd);
  }
}
