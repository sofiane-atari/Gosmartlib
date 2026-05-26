import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { CreateSchoolRequest, OneRosterCredentialValidationRequest, School } from '../../model/school.model';

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

  public disableSchool(id: number): Observable<School> {
    return this.http.put<School>(`${this.url}/${id}/disable`, {});
  }

  public enableSchool(id: number): Observable<School> {
    return this.http.put<School>(`${this.url}/${id}/enable`, {});
  }

  public purgeSchool(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}/purge`);
  }

  public isDevModeEnabled(): Observable<boolean> {
    return this.http.get<{ enabled: boolean }>(`${this.url}/dev-mode-enabled`)
      .pipe(map((response) => response.enabled));
  }

  public getBookCount(id: number): Observable<number> {
    return this.http.get<number>(`${this.url}/${id}/books/count`);
  }

  public getTotalBookCount(): Observable<number> {
    return this.http.get<number>(`${this.url}/books/count`);
  }

  public checkDomain(domainName: string): Observable<void> {
    const params = new HttpParams().set('domainName', domainName);
    return this.http.get<void>(`${this.url}/check-domain`, { params });
  }

  public validateCredentials(dto: OneRosterCredentialValidationRequest): Observable<void> {
    return this.http.post<void>(`${this.url}/validate-credentials`, dto);
  }
}
