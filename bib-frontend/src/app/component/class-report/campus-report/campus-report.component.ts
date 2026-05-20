import { Component, OnInit } from '@angular/core';
import { LoanVolumePeriod, SchoolReport, SchoolReportClassActivity } from '../../../model/school-report.model';
import { SchoolReportService } from '../../../services/school-report/school-report.service';
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
  TrendingUp,
  BarChart3
} from 'lucide-angular';
import { ButtonHoverHelpComponent } from "../../shared/button-hover-help/button-hover-help.component";
import { ToastService } from '../../../services/toast/toast.service';

@Component({
  selector: 'app-campus-report',
  standalone: true,
  templateUrl: './campus-report.component.html',
  imports: [
    CommonModule,
    LucideAngularModule,
    SearchBarComponent,
    PagingComponent,
    ButtonHoverHelpComponent
],
  styleUrls: ['./campus-report.component.css']
})
export class CampusReportComponent implements OnInit {

  report: SchoolReport | null = null;
  loading = false;

  classSearchQuery = '';

  currentClassPage = 0;
  classPageSize = 5;

  readonly icons = {
    library: LibraryBig,
    users: Users,
    bookOpen: BookOpen,
    clock: Clock3,
    star: Star,
    user: UserRound,
    arrowLeft: ArrowLeft,
    chevronDown: ChevronDown,
    trending: TrendingUp,
    chart: BarChart3
  };

  constructor(
    private reportService: SchoolReportService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadReport();
  }

  loadReport(): void {
    this.loading = true;

    this.reportService.getSchoolReport(10).subscribe({
      next: report => {
        this.report = report;
        this.currentClassPage = 0;
        this.loading = false;
      },
      error: error => {
        console.error('Error loading school report:', error);
        this.toastService.show('Fout bij het laden van schoolrapportage', 'error');
        this.loading = false;
      }
    });
  }

  onClassSearchChange(query: string): void {
    this.classSearchQuery = query;
    this.currentClassPage = 0;
  }

  onClassPageChange(page: number): void {
    this.currentClassPage = page;
  }

  getFilteredClasses(): SchoolReportClassActivity[] {
    if (!this.report) {
      return [];
    }

    const query = this.classSearchQuery.trim().toLowerCase();

    if (!query) {
      return this.report.classActivity;
    }

    return this.report.classActivity.filter(classItem =>
      classItem.classTitle.toLowerCase().includes(query)
    );
  }

  getPagedClasses(): SchoolReportClassActivity[] {
    const filteredClasses = this.getFilteredClasses();

    const start = this.currentClassPage * this.classPageSize;
    const end = start + this.classPageSize;

    return filteredClasses.slice(start, end);
  }

  getClassPagingMeta(): PagingMeta {
    const totalItems = this.getFilteredClasses().length;
    const totalPages = Math.ceil(totalItems / this.classPageSize);

    return {
      currentPage: this.currentClassPage,
      pageSize: this.classPageSize,
      totalItems,
      totalPages
    };
  }

  shouldShowClassPaging(): boolean {
    return this.getFilteredClasses().length > this.classPageSize;
  }

  getLinkedPercentage(): number {
    if (!this.report || this.report.totalStudentCount === 0) {
      return 0;
    }

    return Math.round((this.report.classActivity.length / this.report.classCount) * 100);
  }

  getMaxBookLoans(): number {
    return Math.max(...(this.report?.popularBooks.map(book => book.loanCount) ?? [0]), 1);
  }

  getMaxPeriodLoans(): number {
    return Math.max(...(this.report?.loanVolume.map(period => period.loanCount) ?? [0]), 1);
  }

  getBookBarWidth(loanCount: number): string {
    return `${Math.max((loanCount / this.getMaxBookLoans()) * 100, 6)}%`;
  }

  getVolumeBarHeight(period: LoanVolumePeriod): string {
    return `${Math.max((period.loanCount / this.getMaxPeriodLoans()) * 100, 8)}%`;
  }

  formatPeriod(period: string): string {
    const [year, month] = period.split('-').map(Number);
    return new Date(year, month - 1).toLocaleDateString('nl-BE', { month: 'short', year: '2-digit' });
  }

  goBackToReports(): void {
    this.router.navigate(['/reports']);
  }
}



