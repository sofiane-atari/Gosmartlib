import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SchoolReport } from '../../model/school-report.model';

@Injectable({
  providedIn: 'root'
})
export class SchoolReportService {
  private readonly apiUrl = `${environment.apiUrl}/reports/school`;

  constructor(private http: HttpClient) {}

  getSchoolReport(limit = 10, offset = 0, schoolId?: number): Observable<SchoolReport> {
    let params = new HttpParams()
      .set('limit', limit)
      .set('offset', offset);

    if (schoolId !== undefined) {
      params = params.set('schoolId', schoolId);
    }

    return this.http.get<SchoolReport>(this.apiUrl, { params });
  }
}
