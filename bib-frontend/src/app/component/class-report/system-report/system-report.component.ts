import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import {
  ArrowLeft,
  BarChart3,
  BookOpen,
  Clock3,
  LibraryBig,
  LucideAngularModule,
  TrendingUp,
  Users,
} from 'lucide-angular';
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';
import { SystemReport, SystemSchoolActivity } from '../../../model/system-report.model';
import { SystemReportService } from '../../../services/system-report/system-report.service';
import { ToastService } from '../../../services/toast/toast.service';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { PagingComponent, PagingMeta } from '../../shared/paging/paging.component';
import { SearchBarComponent } from '../../shared/search-bar/search-bar.component';
import { StatCardComponent } from "../../shared/stat-card/stat-card.component";

@Component({
  selector: 'app-system-report',
  standalone: true,
  templateUrl: './system-report.component.html',
  imports: [
    CommonModule,
    LucideAngularModule,
    SearchBarComponent,
    PagingComponent,
    ButtonHoverHelpComponent,
    StatCardComponent
],
  styleUrls: ['./system-report.component.css'],
  animations: [fadeInUpAnimation],
})
export class SystemReportComponent implements OnInit {
  report: SystemReport | null = null;
  loading = false;

  schoolSearchQuery = '';
  currentSchoolPage = 0;
  schoolPageSize = 5;

  readonly icons = {
    library: LibraryBig,
    users: Users,
    bookOpen: BookOpen,
    clock: Clock3,
    arrowLeft: ArrowLeft,
    trending: TrendingUp,
    chart: BarChart3,
  };

  constructor(
    private reportService: SystemReportService,
    private toastService: ToastService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadReport();
  }

  loadReport(): void {
    this.loading = true;

    this.reportService.getSystemReport().subscribe({
      next: report => {
        this.report = report;
        this.currentSchoolPage = 0;
        this.loading = false;
      },
      error: error => {
        console.error('Error loading system report:', error);
        this.toastService.show('Fout bij het laden van systeemrapportage', 'error');
        this.loading = false;
      },
    });
  }

  onSchoolSearchChange(query: string): void {
    this.schoolSearchQuery = query;
    this.currentSchoolPage = 0;
  }

  onSchoolPageChange(page: number): void {
    this.currentSchoolPage = page;
  }

  getFilteredSchools(): SystemSchoolActivity[] {
    if (!this.report) {
      return [];
    }

    const query = this.schoolSearchQuery.trim().toLowerCase();

    if (!query) {
      return this.report.schools;
    }

    return this.report.schools.filter(school =>
      school.schoolName.toLowerCase().includes(query)
      || school.syncStatus.toLowerCase().includes(query)
      || (school.syncErrorMessage?.toLowerCase().includes(query) ?? false)
    );
  }

  getPagedSchools(): SystemSchoolActivity[] {
    const filteredSchools = this.getFilteredSchools();
    const start = this.currentSchoolPage * this.schoolPageSize;
    const end = start + this.schoolPageSize;

    return filteredSchools.slice(start, end);
  }

  getSchoolPagingMeta(): PagingMeta {
    const totalItems = this.getFilteredSchools().length;
    const totalPages = Math.ceil(totalItems / this.schoolPageSize);

    return {
      currentPage: this.currentSchoolPage,
      pageSize: this.schoolPageSize,
      totalItems,
      totalPages,
    };
  }

  shouldShowSchoolPaging(): boolean {
    return this.getFilteredSchools().length > this.schoolPageSize;
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

  getLoanVolumeTicks(): number[] {
    return [...this.getYAxisTicks()].reverse();
  }

  getLoanVolumeTickY(index: number): number {
    const top = 20;
    const bottom = 150;
    const steps = 5;

    return top + (index / steps) * (bottom - top);
  }

  getNiceMax(): number {
    const raw = this.getMaxPeriodLoans();
    const niceSteps = [1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000];
    const targetSteps = 5;
    const rawStep = raw / targetSteps;
    const step = niceSteps.find(s => s >= rawStep) ?? 1000;
    return Math.ceil(raw / step) * step;
  }

  getYAxisTicks(): number[] {
    const max = this.getNiceMax();
    const targetSteps = 5;
    const step = max / targetSteps;
    return Array.from({ length: targetSteps + 1 }, (_, i) => i * step);
  }

  getLoanVolumeBarWidth(): number {
    const count = this.report?.loanVolume.length ?? 0;
    if (count === 0) {
      return 0;
    }

    return Math.max(10, Math.min(32, (420 / count) * 0.55));
  }

  getLoanVolumeBarX(index: number): number {
    const count = this.report?.loanVolume.length ?? 1;
    const slotWidth = 420 / count;

    return 52 + index * slotWidth + (slotWidth - this.getLoanVolumeBarWidth()) / 2;
  }

  getLoanVolumeBarY(loanCount: number): number {
    const max = this.getNiceMax();
    const top = 20;
    const bottom = 150;

    return bottom - (loanCount / max) * (bottom - top);
  }

  getLoanVolumeBarHeight(loanCount: number): number {
    if (loanCount === 0) {
      return 0;
    }

    return Math.max(3, 150 - this.getLoanVolumeBarY(loanCount));
  }

  getSyncStatusLabel(status: SystemSchoolActivity['syncStatus']): string {
    if (status === 'ok') {
      return 'OK';
    }

    if (status === 'warning') {
      return 'Waarschuwing';
    }

    return 'Fout';
  }

  getSyncTooltip(school: SystemSchoolActivity): string {
    return school.syncErrorMessage || `Synchronisatiestatus: ${this.getSyncStatusLabel(school.syncStatus)}`;
  }

  getLinkedUsersPercentageBarWidth(): string {
    const percentage = this.report?.linkedUsersPercentage ?? 0;
    return `${Math.max(0, Math.min(percentage, 100))}%`;
  }

  formatTick(value: number): string {
    return parseFloat(value.toFixed(1)).toString();
  }

  formatPeriod(period: string): string {
    const [year, month] = period.split('-').map(Number);
    return new Date(year, month - 1).toLocaleDateString('nl-BE', { month: 'short', year: '2-digit' });
  }

  formatDate(date?: string): string {
    if (!date) {
      return 'Onbekend';
    }

    return new Date(date).toLocaleDateString('nl-BE', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatLastSyncDate(date?: string): string {
    return date ? this.formatDate(date) : 'Nog nooit';
  }

  goBackToReports(): void {
    this.router.navigate(['/reports']);
  }
}
