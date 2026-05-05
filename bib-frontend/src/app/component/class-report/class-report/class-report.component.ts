import { Component, OnInit } from '@angular/core';
import {
  ReportRow,
  ClassReport
} from '../../../model/class-report.model';
import { TeacherClassReportService } from '../../../services/class-report/class-report.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-teacher-class-report',
  standalone: true,
  templateUrl: './class-report.component.html',
  imports: [CommonModule, FormsModule],
  styleUrls: ['./class-report.component.scss']
})
export class ClassReportComponent implements OnInit {

  reports: ClassReport[] = [];
  loading = false;
  errorMessage = '';

  constructor(private reportService: TeacherClassReportService) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading = true;
    this.errorMessage = '';

    this.reportService.getMyClassReports().subscribe({
      next: reports => {
        this.reports = reports;
        this.loading = false;
      },
      error: error => {
        console.error(error);
        this.errorMessage = 'Klasrapporten konden niet geladen worden.';
        this.loading = false;
      }
    });
  }
}