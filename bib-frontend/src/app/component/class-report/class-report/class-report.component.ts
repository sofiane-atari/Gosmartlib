import { Component, OnInit } from '@angular/core';
import { ClassReport, ReportRow } from '../../../model/class-report.model';
import { TeacherClassReportService } from '../../../services/class-report/class-report.service';
import { CommonModule } from '@angular/common';
import { SearchBarComponent } from '../../shared/search-bar/search-bar.component'; // pas path aan indien nodig
import {
  LucideAngularModule,
  BookOpen,
  Users,
  LibraryBig,
  Clock3,
  Star,
  UserRound
} from 'lucide-angular';

@Component({
  selector: 'app-teacher-class-report',
  standalone: true,
  templateUrl: './class-report.component.html',
  imports: [CommonModule, LucideAngularModule, SearchBarComponent],
  styleUrls: ['./class-report.component.css']
})
export class ClassReportComponent implements OnInit {
  reports: ClassReport[] = [];
  loading = false;
  errorMessage = '';

  studentSearchQuery = '';

  readonly icons = {
    library: LibraryBig,
    users: Users,
    bookOpen: BookOpen,
    clock: Clock3,
    star: Star,
    user: UserRound
  };

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

  onStudentSearchChange(query: string): void {
    this.studentSearchQuery = query;
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
}