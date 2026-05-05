import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ClassReport } from '../../model/class-report.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TeacherClassReportService {

  private readonly apiUrl = `${environment.apiUrl}/reports`;

  constructor(private http: HttpClient) {}

  getMyClassReports(): Observable<ClassReport[]> {
    return this.http.get<ClassReport[]>(
      `${this.apiUrl}/my-classes`
    );
  }
}