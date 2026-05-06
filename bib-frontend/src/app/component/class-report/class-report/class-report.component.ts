import { Component, OnInit } from '@angular/core';
import { ClassReport, ReportRow } from '../../../model/class-report.model';
import { TeacherClassReportService } from '../../../services/class-report/class-report.service';
import { CommonModule } from '@angular/common';
import { SearchBarComponent } from '../../shared/search-bar/search-bar.component';
import { PagingComponent, PagingMeta } from '../../shared/paging/paging.component';
import { Router } from '@angular/router';

import {
  LucideAngularModule,
  BookOpen,
  Users,
  LibraryBig,
  Clock3,
  Star,
  UserRound,
  ArrowLeft
} from 'lucide-angular';
import { ButtonHoverHelpComponent } from "../../shared/button-hover-help/button-hover-help.component";
import { ToastService } from '../../../services/toast/toast.service';

@Component({
  selector: 'app-teacher-class-report',
  standalone: true,
  templateUrl: './class-report.component.html',
  imports: [
    CommonModule,
    LucideAngularModule,
    SearchBarComponent,
    PagingComponent,
    ButtonHoverHelpComponent
],
  styleUrls: ['./class-report.component.css']
})
export class ClassReportComponent implements OnInit {

  reports: ClassReport[] = [];
  loading = false;

  studentSearchQuery = '';

  currentStudentPage = 0;
  studentPageSize = 5;

  readonly icons = {
    library: LibraryBig,
    users: Users,
    bookOpen: BookOpen,
    clock: Clock3,
    star: Star,
    user: UserRound,
    arrowLeft: ArrowLeft
  };

  constructor(private reportService: TeacherClassReportService, private toastService: ToastService, private router: Router) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading = true;

    this.reportService.getMyClassReports().subscribe({
      next: reports => {
        this.reports = reports;
        this.currentStudentPage = 0;
        this.loading = false;
      },
      error: error => {
        console.error(error);
        this.toastService.show('Fout bij het laden van rapporten', 'error');
        this.loading = false;
      }
    });
  }

  onStudentSearchChange(query: string): void {
    this.studentSearchQuery = query;
    this.currentStudentPage = 0;
  }

  onStudentPageChange(page: number): void {
    this.currentStudentPage = page;
  }

  getFilteredStudents(report: ClassReport): ReportRow[] {
    const query = this.studentSearchQuery.trim().toLowerCase();

    if (!query) {
      return report.students;
    }

    return report.students.filter(student =>
      student.displayName.toLowerCase().includes(query)
      || student.studentSourcedId.toLowerCase().includes(query)
      || this.getReadingActivity(student).toLowerCase().includes(query)
      || this.getStudentStatus(student).toLowerCase().includes(query)
    );
  }

  getPagedStudents(report: ClassReport): ReportRow[] {
    const filteredStudents = this.getFilteredStudents(report);

    const start = this.currentStudentPage * this.studentPageSize;
    const end = start + this.studentPageSize;

    return filteredStudents.slice(start, end);
  }

  getStudentPagingMeta(report: ClassReport): PagingMeta {
    const totalItems = this.getFilteredStudents(report).length;
    const totalPages = Math.ceil(totalItems / this.studentPageSize);

    return {
      currentPage: this.currentStudentPage,
      pageSize: this.studentPageSize,
      totalItems,
      totalPages
    };
  }

  shouldShowStudentPaging(report: ClassReport): boolean {
    return this.getFilteredStudents(report).length > this.studentPageSize;
  }

  getLinkedPercentage(report: ClassReport): number {
    if (report.studentCount === 0) {
      return 0;
    }

    return Math.round((report.linkedStudentCount / report.studentCount) * 100);
  }

  getReadingActivity(student: { totalLoans: number }): string {
    if (student.totalLoans === 0) {
      return 'Geen activiteit';
    }

    if (student.totalLoans <= 2) {
      return 'Starter';
    }

    if (student.totalLoans <= 5) {
      return 'Actief';
    }

    return 'Sterke lezer';
  }

  getStudentStatus(student: { linkedUserId: string | null; activeLoans: number }): string {
    if (!student.linkedUserId) {
      return 'Niet gekoppeld';
    }

    if (student.activeLoans > 0) {
      return 'Boek terugbrengen';
    }

    return 'In orde';
  }

  goBackToReports(): void {
    this.router.navigate(['/reports']);
  }
}
