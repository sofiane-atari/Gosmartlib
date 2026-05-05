import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Campus, CreateCampusRequest } from '../../model/campus.model';
import { AuthUserDto } from '../../model/auth.model';

@Injectable({
  providedIn: 'root',
})
export class CampusService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/campuses`;

  public getBySchool(schoolId: number): Observable<Campus[]> {
    return this.http.get<Campus[]>(this.url, {
      params: { schoolId },
    });
  }

  public getForCurrentUserSchool(): Observable<Campus[]> {
    return this.http.get<Campus[]>(`${this.url}/me`);
  }

  public getById(id: number): Observable<Campus> {
    return this.http.get<Campus>(`${this.url}/${id}`);
  }

  public create(schoolId: number, campus: CreateCampusRequest): Observable<Campus> {
    return this.http.post<Campus>(this.url, campus, {
      params: { schoolId },
    });
  }

  public update(id: number, campus: CreateCampusRequest): Observable<Campus> {
    return this.http.put<Campus>(`${this.url}/${id}`, campus);
  }

  public switchMyCampus(campusId: number): Observable<AuthUserDto> {
    return this.http.patch<AuthUserDto>(`${environment.apiUrl}/users/me/campus`, {
      campusId,
    });
  }
}
