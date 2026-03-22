export interface Genre {
  name: string;
  color: string;
  description: string;
  booksCount?: number;
}
export interface GenreStats {
  totalItems: number;
  unActiveGenres: number;
}
