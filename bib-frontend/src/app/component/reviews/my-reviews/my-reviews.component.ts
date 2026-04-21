import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule, Star, Trash2, Search, ChevronDown, ChevronUp } from 'lucide-angular';

import { ReviewService } from '../../../services/review-service/review.service';
import { Review } from '../../../model/review.model';
import { ToastService } from '../../../services/toast/toast.service';
import { PagingComponent, PagingMeta } from '../../shared/paging/paging.component';
import {
  fadeInUpAnimation,
  reviewCardAnimation,
} from '../../shared/animations/my-reviews.animations';
import { PaginatedResponse } from '../../../model/shared/paging/PaginatedResponse.model';

@Component({
  selector: 'app-my-reviews',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule, PagingComponent],
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

  protected reviews: Review[] = [];
  protected isLoading = true;

  protected pagingMeta: PagingMeta = {
    currentPage: 0,
    pageSize: 5,
    totalItems: 0,
    totalPages: 0,
  };

  private readonly expandedReviewIds = new Set<number>();
  private readonly reviewService = inject(ReviewService);
  private readonly toastService = inject(ToastService);

  public ngOnInit(): void {
    this.loadMyReviews();
  }

  protected loadMyReviews(page: number = this.pagingMeta.currentPage): void {
    this.isLoading = true;

    this.reviewService
      .getMyReviews(page, this.pagingMeta.pageSize)
      .subscribe({
        next: (response: PaginatedResponse<Review>) => {
          this.reviews = response.data;
          this.pagingMeta = response.meta;
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
            this.expandedReviewIds.delete(reviewId);

            this.reviews = this.reviews.filter(
              (review: Review) => review.id !== reviewId,
            );

            const updatedTotalItems = Math.max(this.pagingMeta.totalItems - 1, 0);
            const updatedTotalPages = updatedTotalItems === 0
              ? 0
              : Math.ceil(updatedTotalItems / this.pagingMeta.pageSize);

            const nextPageToLoad = Math.min(
              this.pagingMeta.currentPage,
              Math.max(updatedTotalPages - 1, 0),
            );

            this.pagingMeta = {
              ...this.pagingMeta,
              totalItems: updatedTotalItems,
              totalPages: updatedTotalPages,
              currentPage: nextPageToLoad,
            };

            const shouldReload =
              this.reviews.length === 0 && updatedTotalItems > 0;

            if (shouldReload) {
              this.loadMyReviews(nextPageToLoad);
            }

            this.toastService.show('Review verwijderd.', 'success');
          },
          error: (error) => {
            console.error('Fout bij verwijderen van review:', error);
            this.toastService.show('De review kon niet verwijderd worden.', 'error');
          },
        });
      },
      () => {},
    );
  }

  protected onPageChange(page: number): void {
    if (page === this.pagingMeta.currentPage) {
      return;
    }

    this.loadMyReviews(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
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
