export interface Review {
  id?: number;
  comment: string;
  rating: number;
  reportCount?: number;
  bookId?: number;
  bookTitle?: string;
  bookAuthor?: string;
  bookImageUrl?: string;
  moderationStatus?: 'ACTIVE' | 'FLAGGED' | 'KEPT';
  reportedByCurrentUser?: boolean;
  createdAt?: string;
  reviewerId?: string;
  reviewerReviewBlocked?: boolean;
  currentUserReview?: boolean;
}

export interface ReviewDashboardStats {
  totalFlaggedReviews: number;
  criticalFlaggedReviews: number;
}
