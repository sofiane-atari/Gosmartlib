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

  getSchoolReport(limit = 10): Observable<SchoolReport> {
    let params = new HttpParams().set('limit', limit);

    return this.http.get<SchoolReport>(this.apiUrl, { params });
  }
}
