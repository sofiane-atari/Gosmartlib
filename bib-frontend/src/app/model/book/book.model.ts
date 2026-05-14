import { Genre } from '../genre/genre.model';
import { ReadingLevel } from '../readingLevel.model';

export interface Book {
  id?: number;
  isbn?: string;
  title: string;
  author: string;
  pages: number;
  imageUrl?: string;
  description?: string;
  releaseYear: number;
  reviewScore?: number;
  totalCopies?: number;
  availableCopies?: number;
  didactic: boolean;
  genres?: Genre[];
  readingLevel?: ReadingLevel;
  campusId?: number;
  campusName?: string;
}
