import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdditionalWork,
  AdditionalWorkCreateRequest,
  CourseWorkDetails,
  CourseWorkDetailsRequest,
  PracticeDetails,
  PracticeDetailsRequest,
  QualificationDetails,
  QualificationDetailsRequest,
} from '../../models/additional-work.model';
import { PageResponse } from '../../models/page.model';

@Injectable({ providedIn: 'root' })
export class AdditionalWorkService {
  private readonly workUrl = '/api/works';

  constructor(private http: HttpClient) {}

  // Additional Work
  getAll(): Observable<AdditionalWork[]> {
    return this.http.get<AdditionalWork[]>(this.workUrl);
  }

  getPage(page: number, size: number, type?: string, commissionId?: number, sortBy?: string, sortDir?: string): Observable<PageResponse<AdditionalWork>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (type) params = params.set('type', type);
    if (commissionId != null) params = params.set('commissionId', commissionId);
    if (sortBy) params = params.set('sortBy', sortBy);
    if (sortDir) params = params.set('sortDir', sortDir);
    return this.http.get<PageResponse<AdditionalWork>>(`${this.workUrl}/paged`, { params });
  }

  getById(id: number): Observable<AdditionalWork> {
    return this.http.get<AdditionalWork>(`${this.workUrl}/${id}`);
  }

  getByCommissionId(commissionId: number): Observable<AdditionalWork[]> {
    return this.http.get<AdditionalWork[]>(`${this.workUrl}/by-commission/${commissionId}`);
  }

  getByBookNumberId(bookNumberId: number): Observable<AdditionalWork[]> {
    return this.http.get<AdditionalWork[]>(`${this.workUrl}/by-book-number/${bookNumberId}`);
  }

  create(request: AdditionalWorkCreateRequest): Observable<AdditionalWork> {
    return this.http.post<AdditionalWork>(this.workUrl, request);
  }

  update(id: number, request: AdditionalWorkCreateRequest): Observable<AdditionalWork> {
    return this.http.put<AdditionalWork>(`${this.workUrl}/${id}`, request);
  }

  grade(id: number, universityGrade: number | null, ectsGrade: string | null, nationalGrade: string | null): Observable<AdditionalWork> {
    return this.http.patch<AdditionalWork>(`${this.workUrl}/${id}/grade`, { universityGrade, ectsGrade, nationalGrade });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.workUrl}/${id}`);
  }

  // Course Work Details
  createCourseWorkDetails(workId: number, request: CourseWorkDetailsRequest): Observable<CourseWorkDetails> {
    return this.http.post<CourseWorkDetails>(`${this.workUrl}/${workId}/course-work`, request);
  }

  updateCourseWorkDetails(workId: number, request: CourseWorkDetailsRequest): Observable<CourseWorkDetails> {
    return this.http.put<CourseWorkDetails>(`${this.workUrl}/${workId}/course-work`, request);
  }

  // Practice Details
  createPracticeDetails(workId: number, request: PracticeDetailsRequest): Observable<PracticeDetails> {
    return this.http.post<PracticeDetails>(`${this.workUrl}/${workId}/practice`, request);
  }

  updatePracticeDetails(workId: number, request: PracticeDetailsRequest): Observable<PracticeDetails> {
    return this.http.put<PracticeDetails>(`${this.workUrl}/${workId}/practice`, request);
  }

  // Qualification Details
  createQualificationDetails(workId: number, request: QualificationDetailsRequest): Observable<QualificationDetails> {
    return this.http.post<QualificationDetails>(`${this.workUrl}/${workId}/qualification`, request);
  }

  updateQualificationDetails(workId: number, request: QualificationDetailsRequest): Observable<QualificationDetails> {
    return this.http.put<QualificationDetails>(`${this.workUrl}/${workId}/qualification`, request);
  }
}
