import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { School } from '../../model/school.model';

@Injectable({
  providedIn: 'root',
})
export class SchoolService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/schools`;

  public getAll(): Observable<School[]> {
    return this.http.get<School[]>(this.url);
  }

  public create(school: School): Observable<School> {
    return this.http.post<School>(this.url, school);
  }
}
