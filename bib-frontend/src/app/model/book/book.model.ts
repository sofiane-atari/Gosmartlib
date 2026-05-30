import { Genre } from '../genre/genre.model';
import { ReadingLevel } from '../readingLevel.model';
import { Tag } from '../tag/tag.model';

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
  tags?: Tag[];
  readingLevel?: ReadingLevel;
  campusId?: number;
  campusName?: string;
  availableCampuses?: string[];
  copyLocations?: BookCopyCampusLocation[];
  favorite?: boolean;
}

export interface BookCopyCampusLocation {
  campusId: number;
  campusName: string;
  address?: string | null;
  postalCode?: number | null;
  copies: BookCopyLocation[];
}

export interface BookCopyLocation {
  id: number;
  availability: 'AVAILABLE' | 'BORROWED' | string;
  condition?: BookCondition;
  expectedReturnDate?: string | null;
  campusId?: number;
  campusName?: string;
}

export type BookCondition = 'GOOD_CONDITION' | 'DAMAGED' | 'LOST';
