export interface Label {
  name: string;
  color: string;
  description: string;
  booksCount?: number;
}
export interface LabelStats {
  totalItems: number;
  unActiveLabels: number;
}
