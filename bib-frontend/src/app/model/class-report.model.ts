export interface ReportRow {
  studentSourcedId: string;
  displayName: string;
  totalItems: number;
  completedItems: number;
  progressPercentage: number;
  averageScore: number;
  totalTimeMinutes: number;
}

export interface ClassReport {
  classSourcedId: string;
  classTitle: string;
  studentCount: number;
  classAverageScore: number;
  classAverageProgress: number;
  students: ReportRow[];
}