import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SystemReport } from '../../model/system-report.model';

@Injectable({
  providedIn: 'root'
})
export class SystemReportService {
  private readonly apiUrl = `${environment.apiUrl}/reports/system`;

  constructor(private http: HttpClient) {}

  getSystemReport(): Observable<SystemReport> {
    return this.http.get<SystemReport>(this.apiUrl);
  }
}
