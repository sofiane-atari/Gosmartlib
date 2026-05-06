export interface PopularGenre {
  genreId: number;
  name: string;
  color: string | null;
  loanCount: number;
}

export interface TopRatedBookReport {
  bookId: number;
  title: string;
  author: string;
  imageUrl: string | null;
  averageRating: number;
  reviewCount: number;
}

export interface CampusReportRow {
  classSourcedId: string;
  classTitle: string;

  studentCount: number;
  linkedStudentCount: number;

  totalLoans: number;
  activeLoans: number;
  averageLoansPerLinkedStudent: number;

  classAverageScore: number;
  classAverageProgress: number;
}

export interface CampusReport {
  campusId: number;
  campusSourcedId: string;
  campusName: string;

  classCount: number;
  totalStudentCount: number;
  linkedStudentCount: number;

  totalLoans: number;
  activeLoans: number;
  averageLoansPerLinkedStudent: number;

  campusAverageScore: number;
  campusAverageProgress: number;

  popularGenres: PopularGenre[];
  topRatedBooks: TopRatedBookReport[];
  classes: CampusReportRow[];
}


