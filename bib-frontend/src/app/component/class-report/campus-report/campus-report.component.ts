import { Component, OnInit } from '@angular/core';
import { CampusReport, CampusReportRow } from '../../../model/campus-report.model';
import { CampusReportService } from '../../../services/campus-report/campus-report.service';
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
  ChevronDown
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

  report: CampusReport | null = null;
  allCampusReports: CampusReport[] = [];
  loading = false;
  campusDropdownOpen = false;

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
    chevronDown: ChevronDown
  };

  constructor(
    private reportService: CampusReportService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loading = true;

    // First load the current user's campus report
    this.reportService.getMyCampusReport().subscribe({
      next: report => {
        this.report = report;
        this.currentClassPage = 0;
        this.loading = false;

        // Then load all campuses for switching
        this.loadAllCampuses();
      },
      error: error => {
        console.error('Error loading campus report:', error);
        this.toastService.show('Fout bij het laden van campusrapport', 'error');
        this.loading = false;
      }
    });
  }

  loadAllCampuses(): void {
    this.reportService.getAllCampusReports().subscribe({
      next: reports => {
        this.allCampusReports = reports;
      },
      error: error => {
        console.error('Error loading campuses:', error);
        // Don't show error toast for this, as it's secondary functionality
      }
    });
  }

  switchCampus(campus: CampusReport): void {
    // Check if switching to the same campus
    if (campus.campusId === this.report?.campusId) {
      this.campusDropdownOpen = false;
      return;
    }

    this.loading = true;

    // We need to fetch the specific campus report
    this.reportService.getCampusReportById(campus.campusId).subscribe({
      next: report => {
        this.report = report;
        this.classSearchQuery = '';
        this.currentClassPage = 0;
        this.campusDropdownOpen = false;
        this.loading = false;
      },
      error: error => {
        console.error('Error switching campus:', error);
        this.toastService.show('Fout bij het laden van campus rapportage', 'error');
        this.loading = false;
      }
    });
  }

  toggleCampusDropdown(): void {
    this.campusDropdownOpen = !this.campusDropdownOpen;
  }

  onClassSearchChange(query: string): void {
    this.classSearchQuery = query;
    this.currentClassPage = 0;
  }

  onClassPageChange(page: number): void {
    this.currentClassPage = page;
  }

  getFilteredClasses(): CampusReportRow[] {
    if (!this.report) {
      return [];
    }

    const query = this.classSearchQuery.trim().toLowerCase();

    if (!query) {
      return this.report.classes;
    }

    return this.report.classes.filter(classItem =>
      classItem.classTitle.toLowerCase().includes(query)
      || classItem.classSourcedId.toLowerCase().includes(query)
    );
  }

  getPagedClasses(): CampusReportRow[] {
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

    return Math.round((this.report.linkedStudentCount / this.report.totalStudentCount) * 100);
  }

  goBackToReports(): void {
    this.router.navigate(['/reports']);
  }
}



