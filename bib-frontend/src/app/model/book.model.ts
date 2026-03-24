import { Genre } from "./genre.model";
import { ReadingLevel } from "./readingLevel.model";

export interface Book {
  id?: number;
  isbn?: string;
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
  readingLevel?: ReadingLevel;
}
