export interface Review {
  id?: number;
  comment: string;
  rating: number;
  reportCount?: number;
  bookId?: number;
  bookTitle?: string;
  bookAuthor?: string;
  moderationStatus?: 'ACTIVE' | 'FLAGGED' | 'KEPT';
  reportedByCurrentUser?: boolean;
}