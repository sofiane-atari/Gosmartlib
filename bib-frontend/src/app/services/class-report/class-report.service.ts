import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ClassReport, ReportClassOption } from '../../model/class-report.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TeacherClassReportService {

  private readonly apiUrl = `${environment.apiUrl}/reports`;

  constructor(private http: HttpClient) {}

  public getAvailableClasses(): Observable<ReportClassOption[]> {
    return this.http.get<ReportClassOption[]>(`${this.apiUrl}/classes`);
  }

  public getClassReport(classSourcedId: string): Observable<ClassReport> {
    return this.http.get<ClassReport>(`${this.apiUrl}/class`, {
      params: {
        classSourcedId,
      },
    });
  }
}