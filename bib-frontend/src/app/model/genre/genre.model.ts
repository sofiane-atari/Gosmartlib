export interface Genre {
  id?: number;
  name: string;
  color: string;
  description: string;
  booksCount?: number;
  subGenres?: Genre[];
}
