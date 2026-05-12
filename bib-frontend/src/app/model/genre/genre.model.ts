export interface Genre {
  id?: number;
  name: string;
  color: string;
  description: string;
  booksCount?: number;
  subGenres?: Genre[];
}
export interface CreateGenreRequest {
  name: string;
  description: string;
  color: string;
}

export interface UpdateGenreRequest {
  id: number;
  name: string;
  description: string;
  color: string;
}