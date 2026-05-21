import { PopularGenre, TopRatedBookReport } from './class-report.model';

export interface PopularLoanedBook {
  bookId: number;
  title: string;
  author: string | null;
  imageUrl: string | null;
  loanCount: number;
}

export interface LoanVolumePeriod {
  period: string;
  loanCount: number;
}

export interface SchoolReportClassActivity {
  classTitle: string;
  studentCount: number;
  totalLoans: number;
  activeLoans: number;
  averageLoansPerStudent: number;
}

export interface SchoolReportCampusOption {
  campusId: number;
  campusName: string;
}

export interface SchoolReport {
  schoolId: number;
  schoolName: string;
  selectedCampusId: number | null;
  selectedCampusName: string | null;
  campuses: SchoolReportCampusOption[];
  totalLoans: number;
  activeLoans: number;
  classCount: number;
  totalStudentCount: number;
  loansPerStudent: number;
  popularGenres: PopularGenre[];
  topRatedBooks: TopRatedBookReport[];
  popularBooks: PopularLoanedBook[];
  loanVolume: LoanVolumePeriod[];
  classActivity: SchoolReportClassActivity[];
}
