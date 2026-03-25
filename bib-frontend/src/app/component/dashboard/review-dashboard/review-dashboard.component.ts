import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  Check,
  Trash2,
  Flag,
  CircleAlert,
  Star,
} from 'lucide-angular';

import { Review } from '../../../model/review.model'; 
import { ReviewService } from '../../../services/review-service/review.service'; 
import { ToastService } from '../../../services/toast/toast.service'; 

@Component({
  selector: 'app-review-dashboard',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './review-dashboard.component.html',
  styleUrl: './review-dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewDashboardComponent {
  private readonly reviewService = inject(ReviewService);
  private readonly toastService = inject(ToastService);

  protected readonly Check = Check;
  protected readonly Trash2 = Trash2;
  protected readonly Flag = Flag;
  protected readonly CircleAlert = CircleAlert;
  protected readonly Star = Star;

  protected readonly reviews = signal<Review[]>([]);
  protected readonly selectedReviewIds = signal<Set<number>>(new Set());
  protected readonly isLoading = signal<boolean>(false);

  protected readonly selectedCount = computed(
    () => this.selectedReviewIds().size,
  );

  protected readonly criticalReviewsCount = computed(
    () => this.reviews().filter((review) => (review.reportCount ?? 0) >= 5).length,
  );

  protected readonly starValues = [1, 2, 3, 4, 5];

  public ngOnInit(): void {
    this.loadFlaggedReviews();
  }

  protected isSelected(reviewId: number): boolean {
    return this.selectedReviewIds().has(reviewId);
  }

  protected toggleSelection(reviewId: number): void {
    const nextSelectedReviewIds = new Set(this.selectedReviewIds());

    if (nextSelectedReviewIds.has(reviewId)) {
      nextSelectedReviewIds.delete(reviewId);
    } else {
      nextSelectedReviewIds.add(reviewId);
    }

    this.selectedReviewIds.set(nextSelectedReviewIds);
  }

  protected approveReview(reviewId: number): void {
    this.toastService.showConfirm(
      'Weet je zeker dat je deze review wilt behouden?',
      () => {
        this.reviewService.keepReview(reviewId).subscribe({
          next: () => {
            this.removeReviewFromDashboard(reviewId);
            this.toastService.show('Review goedgekeurd.', 'success');
          },
          error: (error) => {
            console.error('Fout bij goedkeuren review', error);
            this.toastService.show(
              'De review kon niet goedgekeurd worden.',
              'error',
            );
          },
        });
      },
      () => {},
    );
  }

  protected deleteReview(reviewId: number): void {
    this.toastService.showConfirm(
      'Weet je zeker dat je deze review wilt verwijderen?',
      () => {
        this.reviewService.deleteReview(reviewId).subscribe({
          next: () => {
            this.removeReviewFromDashboard(reviewId);
            this.toastService.show('Review verwijderd.', 'success');
          },
          error: (error) => {
            console.error('Fout bij verwijderen review', error);
            this.toastService.show(
              'De review kon niet verwijderd worden.',
              'error',
            );
          },
        });
      },
      () => {},
    );
  }

  protected approveSelectedReviews(): void {
    const reviewIds = Array.from(this.selectedReviewIds());

    if (reviewIds.length === 0) {
      return;
    }

    this.processBulkAction(
      reviewIds,
      (reviewId) => this.reviewService.keepReview(reviewId),
      'review goedgekeurd',
      'reviews goedgekeurd',
    );
  }

  protected deleteSelectedReviews(): void {
    const reviewIds = Array.from(this.selectedReviewIds());

    if (reviewIds.length === 0) {
      return;
    }

    this.processBulkAction(
      reviewIds,
      (reviewId) => this.reviewService.deleteReview(reviewId),
      'review verwijderd',
      'reviews verwijderd',
    );
  }

  private loadFlaggedReviews(): void {
    this.isLoading.set(true);

    this.reviewService.getFlaggedReviews().subscribe({
      next: (reviews) => {
        this.reviews.set(reviews);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Fout bij ophalen van flagged reviews', error);
        this.toastService.show(
          'De gerapporteerde reviews konden niet geladen worden.',
          'error',
        );
        this.isLoading.set(false);
      },
    });
  }

  private removeReviewFromDashboard(reviewId: number): void {
    this.reviews.update((currentReviews) =>
      currentReviews.filter((review) => review.id !== reviewId),
    );

    const nextSelectedReviewIds = new Set(this.selectedReviewIds());
    nextSelectedReviewIds.delete(reviewId);
    this.selectedReviewIds.set(nextSelectedReviewIds);
  }

  private processBulkAction(
    reviewIds: number[],
    action: (reviewId: number) => { subscribe: Function },
    singularSuccessMessage: string,
    pluralSuccessMessage: string,
  ): void {
    let processedCount = 0;
    let successCount = 0;
    const totalCount = reviewIds.length;

    reviewIds.forEach((reviewId) => {
      action(reviewId).subscribe({
        next: () => {
          this.removeReviewFromDashboard(reviewId);
          successCount++;
          processedCount++;
          this.handleBulkActionCompletion(
            processedCount,
            totalCount,
            successCount,
            singularSuccessMessage,
            pluralSuccessMessage,
          );
        },
        error: (error: unknown) => {
          console.error('Fout bij bulkactie voor review', reviewId, error);
          processedCount++;
          this.handleBulkActionCompletion(
            processedCount,
            totalCount,
            successCount,
            singularSuccessMessage,
            pluralSuccessMessage,
          );
        },
      });
    });
  }

  private handleBulkActionCompletion(
    processedCount: number,
    totalCount: number,
    successCount: number,
    singularSuccessMessage: string,
    pluralSuccessMessage: string,
  ): void {
    if (processedCount !== totalCount) {
      return;
    }

    this.selectedReviewIds.set(new Set());

    if (successCount === 0) {
      this.toastService.show('Geen reviews konden verwerkt worden.', 'error');
      return;
    }

    if (successCount === 1) {
      this.toastService.show(`1 ${singularSuccessMessage}.`, 'success');
      return;
    }

    this.toastService.show(`${successCount} ${pluralSuccessMessage}.`, 'success');
  }
}