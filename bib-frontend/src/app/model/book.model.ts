import { Genre } from "./genre.model";

export interface Book {
  id?: number;
  ISBN?: string;
  title: string;
  author: string;
  pages: number;
  imageUrl: string;
  description: string;
  releaseYear: number;
  reviewScore?: number;
  totalCopies?: number;
  availableCopies?: number;
  genres: Genre[];
}
