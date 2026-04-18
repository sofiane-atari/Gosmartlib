export interface Genre {
  id: number;
  name: string;
  color: string;
  description: string;
  booksCount?: number;
}
export interface GenreStats {
  totalItems: number;
  unActiveGenres: number;
}
