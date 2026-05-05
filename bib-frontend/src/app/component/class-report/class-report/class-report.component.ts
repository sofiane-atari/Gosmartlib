import { Component, OnInit } from '@angular/core';
import { ClassReport } from '../../../model/class-report.model';
import { TeacherClassReportService } from '../../../services/class-report/class-report.service';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  BookOpen,
  Users,
  LibraryBig,
  Clock3,
  Star,
  Trophy,
  Tags,
  UserRound
} from 'lucide-angular';
@Component({
  selector: 'app-teacher-class-report',
  standalone: true,
  templateUrl: './class-report.component.html',
  imports: [CommonModule, LucideAngularModule],
  styleUrls: ['./class-report.component.css']
})
export class ClassReportComponent implements OnInit {

  reports: ClassReport[] = [];
  loading = false;
  errorMessage = '';

  constructor(private reportService: TeacherClassReportService) {}
  
  readonly icons = {
    library: LibraryBig,
    users: Users,
    bookOpen: BookOpen,
    clock: Clock3,
    star: Star,
    trophy: Trophy,
    tags: Tags,
    user: UserRound
  };

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

  getLinkedPercentage(report: ClassReport): number {
    if (report.studentCount === 0) {
      return 0;
    }

    return Math.round((report.linkedStudentCount / report.studentCount) * 100);
  }

  getReturnRate(student: { totalLoans: number; returnedLoans: number }): number {
    if (student.totalLoans === 0) {
      return 0;
    }

    return Math.round((student.returnedLoans / student.totalLoans) * 100);
  }

  getReadingActivity(student: { totalLoans: number }): string {
    if (student.totalLoans === 0) {
      return 'Geen activiteit';
    }

    if (student.totalLoans <= 2) {
      return 'Starter';
    }

    if (student.totalLoans <= 5) {
      return 'Actieve lezer';
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