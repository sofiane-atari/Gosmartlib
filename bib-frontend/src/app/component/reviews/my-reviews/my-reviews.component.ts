import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule, Star, Trash2, Search, ChevronDown, ChevronUp } from 'lucide-angular';

import { ReviewService } from '../../../services/review-service/review.service';
import { Review } from '../../../model/review.model';
import { ToastService } from '../../../services/toast/toast.service';
import {
  fadeInUpAnimation,
  reviewCardAnimation,
} from '../../shared/animations/my-reviews.animations';

@Component({
  selector: 'app-my-reviews',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './my-reviews.component.html',
  styleUrl: './my-reviews.component.css',
  animations: [reviewCardAnimation, fadeInUpAnimation],
})
export class MyReviewsComponent implements OnInit {
  protected readonly Star = Star;
  protected readonly Trash2 = Trash2;
  protected readonly Search = Search;
  protected readonly ChevronDown = ChevronDown;
  protected readonly ChevronUp = ChevronUp;

  protected readonly starValues = [1, 2, 3, 4, 5];
  protected readonly skeletonItems = Array.from({ length: 3 });
  protected readonly maxCollapsedLength = 220;

  private readonly expandedReviewIds = new Set<number>();

  protected reviews: Review[] = [];
  protected isLoading = true;

  private readonly reviewService = inject(ReviewService);
  private readonly toastService = inject(ToastService);

  public ngOnInit(): void {
    this.loadMyReviews();
  }

  protected loadMyReviews(): void {
    this.isLoading = true;

    this.reviewService.getMyReviews().subscribe({
      next: (reviews) => {
        this.reviews = [...reviews].sort(
          (firstReview, secondReview) =>
            (secondReview.id ?? 0) - (firstReview.id ?? 0),
        );
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Fout bij ophalen van mijn reviews:', error);
        this.toastService.show('Je reviews konden niet geladen worden.', 'error');
        this.isLoading = false;
      },
    });
  }

  protected deleteReview(reviewId?: number): void {
    if (!reviewId) {
      return;
    }

    this.toastService.showConfirm(
      'Weet je zeker dat je deze review wilt verwijderen?',
      () => {
        this.reviewService.deleteReview(reviewId).subscribe({
          next: () => {
            this.reviews = this.reviews.filter((review) => review.id !== reviewId);
            this.toastService.show('Review verwijderd.', 'success');
          },
          error: (error) => {
            console.error('Fout bij verwijderen van review:', error);
            this.toastService.show('De review kon niet verwijderd worden.', 'error');
          },
        });
      },
      () => {}
    );
  }

  protected isReviewCollapsible(comment: string | undefined): boolean {
    return (comment?.trim().length ?? 0) > this.maxCollapsedLength;
  }

  protected isReviewExpanded(reviewId: number | undefined): boolean {
    return reviewId !== undefined && this.expandedReviewIds.has(reviewId);
  }

  protected toggleReviewExpanded(reviewId: number | undefined): void {
    if (reviewId === undefined) {
      return;
    }

    if (this.expandedReviewIds.has(reviewId)) {
      this.expandedReviewIds.delete(reviewId);
      return;
    }

    this.expandedReviewIds.add(reviewId);
  }

  protected getDisplayedComment(review: Review): string {
    const comment = review.comment?.trim() ?? '';

    if (!this.isReviewCollapsible(comment) || this.isReviewExpanded(review.id)) {
      return comment;
    }

    return `${comment.slice(0, this.maxCollapsedLength).trimEnd()}...`;
  }

  protected translateStatus(status: string | undefined): string {
    switch (status) {
      case 'ACTIVE':
        return 'Actief';
      case 'FLAGGED':
        return 'Gerapporteerd';
      case 'KEPT':
        return 'Behouden';
      default:
        return status ?? 'Onbekend';
    }
  }
}