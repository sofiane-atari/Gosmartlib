import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CampusReport } from '../../model/campus-report.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CampusReportService {

  private readonly apiUrl = `${environment.apiUrl}/reports`;

  constructor(private http: HttpClient) {}

  getMyCampusReport(): Observable<CampusReport> {
    return this.http.get<CampusReport>(
      `${this.apiUrl}/my-campus`
    );
  }

  getAllCampusReports(): Observable<CampusReport[]> {
    return this.http.get<CampusReport[]>(
      `${this.apiUrl}/all-campuses`
    );
  }

  getCampusReportById(campusId: number): Observable<CampusReport> {
    return this.http.get<CampusReport>(
      `${this.apiUrl}/campus/${campusId}`
    );
  }
}



