import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { from, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateLessonTipRequest,
  LessonTip,
} from '../../model/lesson-tip/lesson-tip.model';

@Injectable({
  providedIn: 'root',
})
export class LessonTipService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/lesson-tips`;

  public getAll(bookId?: number): Observable<LessonTip[]> {
    let params = new HttpParams();

    if (bookId !== undefined) {
      params = params.set('bookId', bookId);
    }

    return this.http.get<LessonTip[]>(this.baseUrl, { params });
  }

  public getById(id: number): Observable<LessonTip> {
    return this.http.get<LessonTip>(`${this.baseUrl}/${id}`);
  }

  public create(request: CreateLessonTipRequest): Observable<LessonTip> {
    return this.http.post<LessonTip>(this.baseUrl, request);
  }

  public update(
    id: number,
    request: CreateLessonTipRequest,
  ): Observable<LessonTip> {
    return this.http.put<LessonTip>(`${this.baseUrl}/${id}`, request);
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
