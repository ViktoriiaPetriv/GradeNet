import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BookNumber, BookNumberRequest } from '../../models/book.model';
import { PageResponse } from '../../models/page.model';

@Injectable({ providedIn: 'root' })
export class BookService {
  private readonly apiUrl = '/api/books';

  constructor(private http: HttpClient) {}

  findAll(number?: string, page = 0, size = 20): Observable<PageResponse<BookNumber>> {
    let params = new HttpParams().set('pageNumber', page).set('size', size);
    if (number) params = params.set('number', number);
    return this.http.get<PageResponse<BookNumber>>(this.apiUrl, { params });
  }

  findById(id: number): Observable<BookNumber> {
    return this.http.get<BookNumber>(`${this.apiUrl}/${id}`);
  }

  findByStudentId(studentId: number): Observable<BookNumber[]> {
    return this.http.get<BookNumber[]>(`${this.apiUrl}/student/${studentId}`);
  }

  create(request: BookNumberRequest): Observable<BookNumber> {
    return this.http.post<BookNumber>(this.apiUrl, request);
  }

  update(id: number, request: BookNumberRequest): Observable<BookNumber> {
    return this.http.put<BookNumber>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  markAsFilled(id: number): Observable<BookNumber> {
    return this.http.patch<BookNumber>(`${this.apiUrl}/${id}/fill`, {});
  }

  markAsHanded(id: number): Observable<BookNumber> {
    return this.http.patch<BookNumber>(`${this.apiUrl}/${id}/hand`, {});
  }
}
