export interface Genre {
  name: string;
  color: string;
  description: string;
  booksCount?: number;
  subGenres?: Genre[];
}
