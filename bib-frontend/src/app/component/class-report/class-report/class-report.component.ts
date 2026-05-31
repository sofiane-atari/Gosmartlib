import { Component, OnInit } from '@angular/core';
import {
  ClassReport,
  ReportClassOption,
  ReportRow,
} from '../../../model/class-report.model';
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
  ArrowLeft,
  ChevronDown,
} from 'lucide-angular';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { ToastService } from '../../../services/toast/toast.service';
import { StatCardComponent } from "../../shared/stat-card/stat-card.component";
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';

@Component({
  selector: 'app-teacher-class-report',
  standalone: true,
  templateUrl: './class-report.component.html',
  imports: [
    CommonModule,
    LucideAngularModule,
    SearchBarComponent,
    PagingComponent,
    ButtonHoverHelpComponent,
    StatCardComponent
],
  styleUrls: ['./class-report.component.css'],
  animations: [fadeInUpAnimation],
})
export class ClassReportComponent implements OnInit {
  classOptions: ReportClassOption[] = [];
  selectedClassSourcedId = '';
  selectedReport: ClassReport | null = null;

  loading = false;
  loadingClasses = false;

  studentSearchQuery = '';

  currentStudentPage = 0;
  studentPageSize = 5;

  isClassDropdownOpen = false;


  readonly icons = {
    library: LibraryBig,
    users: Users,
    bookOpen: BookOpen,
    clock: Clock3,
    star: Star,
    user: UserRound,
    arrowLeft: ArrowLeft,
    chevronDown: ChevronDown,
  };

  constructor(
    private reportService: TeacherClassReportService,
    private toastService: ToastService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadClassOptions();
  }

  loadClassOptions(): void {
    this.loadingClasses = true;
    this.loading = true;

    this.reportService.getAvailableClasses().subscribe({
      next: classOptions => {
        this.classOptions = classOptions;
        this.loadingClasses = false;

        if (classOptions.length === 0) {
          this.selectedReport = null;
          this.loading = false;
          return;
        }

        const firstClass = classOptions[0];
        this.selectedClassSourcedId = firstClass.sourcedId;
        this.loadReport(firstClass.sourcedId);
      },
      error: error => {
        console.error(error);
        this.toastService.show('Fout bij het laden van klassen', 'error');
        this.selectedReport = null;
        this.loadingClasses = false;
        this.loading = false;
      },
    });
  }

  loadReport(classSourcedId: string): void {
    this.loading = true;
    this.currentStudentPage = 0;
    this.studentSearchQuery = '';

    this.reportService.getClassReport(classSourcedId).subscribe({
      next: report => {
        this.selectedReport = report;
        this.loading = false;
      },
      error: error => {
        console.error(error);
        this.toastService.show('Fout bij het laden van het klasrapport', 'error');
        this.selectedReport = null;
        this.loading = false;
      },
    });
  }

  onClassChange(classSourcedId: string): void {
    if (!classSourcedId || classSourcedId === this.selectedClassSourcedId) {
      return;
    }

    this.selectedClassSourcedId = classSourcedId;
    this.loadReport(classSourcedId);
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
      totalPages,
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

  toggleClassDropdown(): void {
    if (this.loadingClasses || this.classOptions.length === 0) {
      return;
    }

    this.isClassDropdownOpen = !this.isClassDropdownOpen;
  }

  selectClass(classSourcedId: string): void {
    this.isClassDropdownOpen = false;
    this.onClassChange(classSourcedId);
  }

  getSelectedClassTitle(): string {
    const selectedClass = this.classOptions.find(
      classOption => classOption.sourcedId === this.selectedClassSourcedId
    );

    return selectedClass?.title ?? 'Kies een klas';
  }
}