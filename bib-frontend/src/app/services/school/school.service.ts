import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateSchoolRequest, School } from '../../model/school.model';

@Injectable({
  providedIn: 'root',
})
export class SchoolService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/schools`;

  public getAll(): Observable<School[]> {
    return this.http.get<School[]>(this.url);
  }

  public create(school: CreateSchoolRequest): Observable<School> {
    return this.http.post<School>(this.url, school);
  }

  public update(id: number, school: CreateSchoolRequest): Observable<School> {
    return this.http.put<School>(`${this.url}/${id}`, school);
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  public getBookCount(id: number): Observable<number> {
    return this.http.get<number>(`${this.url}/${id}/books/count`);
  }

  public getTotalBookCount(): Observable<number> {
    return this.http.get<number>(`${this.url}/books/count`);
  }
}
