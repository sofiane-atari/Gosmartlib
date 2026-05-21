import { LoanVolumePeriod, PopularLoanedBook } from './school-report.model';

export type PopularBook = PopularLoanedBook;

export interface SystemReport {
  totalLoans: number;
  activeLoans: number;
  totalSchools: number;
  activeUsers: number;
  totalBooks: number;
  totalCopies: number;
  linkedUsersPercentage: number;
  loanVolume: LoanVolumePeriod[];
  popularBooks: PopularBook[];
  schools: SystemSchoolActivity[];
}

export interface SystemSchoolActivity {
  schoolId: number;
  schoolName: string;
  logoUrl?: string;
  totalStudents: number;
  linkedStudents: number;
  totalLoans: number;
  activeLoans: number;
  averageLoansPerStudent: number;
  lastSyncDate?: string;
  syncStatus: 'ok' | 'warning' | 'error';
  syncErrorMessage?: string;
}
