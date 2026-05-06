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

export interface ReportRow {
  studentSourcedId: string;
  displayName: string;

  totalItems: number;
  completedItems: number;
  progressPercentage: number;
  averageScore: number;
  totalTimeMinutes: number;

  linkedUserId: string | null;
  totalLoans: number;
  activeLoans: number;
  returnedLoans: number;
  reviewCount: number;
}

export interface ClassReport {
  classSourcedId: string;
  classTitle: string;

  studentCount: number;
  linkedStudentCount: number;

  totalLoans: number;
  activeLoans: number;
  averageLoansPerLinkedStudent: number;

  classAverageScore: number;
  classAverageProgress: number;

  popularGenres: PopularGenre[];
  topRatedBooks: TopRatedBookReport[];
  students: ReportRow[];
}