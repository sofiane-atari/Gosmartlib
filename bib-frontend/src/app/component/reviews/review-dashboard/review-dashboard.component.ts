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
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';

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
  protected readonly isBulkProcessing = signal<boolean>(false);

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
      'Weet je zeker dat je deze review wilt goedkeuren?',
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

    if (reviewIds.length === 0 || this.isBulkProcessing()) {
      return;
    }

    this.toastService.showConfirm(
      `Weet je zeker dat je ${reviewIds.length} review${reviewIds.length > 1 ? 's' : ''} wilt goedkeuren?`,
      () => {
        this.processBulkAction(
          reviewIds,
          (reviewId: number) => this.reviewService.keepReview(reviewId),
          'review goedgekeurd.',
          'reviews goedgekeurd.',
        );
      },
      () => {},
    );
  }

  protected deleteSelectedReviews(): void {
    const reviewIds = Array.from(this.selectedReviewIds());

    if (reviewIds.length === 0 || this.isBulkProcessing()) {
      return;
    }

    this.toastService.showConfirm(
      `Weet je zeker dat je ${reviewIds.length} review${reviewIds.length > 1 ? 's' : ''} wilt verwijderen?`,
      () => {
        this.processBulkAction(
          reviewIds,
          (reviewId: number) => this.reviewService.deleteReview(reviewId),
          'review verwijderd.',
          'reviews verwijderd.',
        );
      },
      () => {},
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
    action: (reviewId: number) => Observable<unknown>,
    singularSuccessMessage: string,
    pluralSuccessMessage: string,
  ): void {
    this.isBulkProcessing.set(true);

    const requests = reviewIds.map((reviewId: number) =>
      action(reviewId).pipe(
        map(() => ({
          reviewId,
          success: true as const,
        })),
        catchError((error: unknown) => {
          console.error('Fout bij bulkactie voor review', reviewId, error);

          return of({
            reviewId,
            success: false as const,
          });
        }),
      ),
    );

    forkJoin(requests)
      .pipe(
        finalize(() => this.isBulkProcessing.set(false)),
      )
      .subscribe({
        next: (results) => {
          const successfulReviewIds = results
            .filter((result) => result.success)
            .map((result) => result.reviewId);

          successfulReviewIds.forEach((reviewId: number) => {
            this.removeReviewFromDashboard(reviewId);
          });

          this.selectedReviewIds.set(new Set());

          const successCount = successfulReviewIds.length;

          if (successCount === 0) {
            this.toastService.show('Geen reviews konden verwerkt worden.', 'error');
            return;
          }

          if (successCount === 1) {
            this.toastService.show(`1 ${singularSuccessMessage}`, 'success');
            return;
          }

          if (successCount < reviewIds.length) {
            this.toastService.show(
              `${successCount} van de ${reviewIds.length} reviews verwerkt.`,
              'success',
            );
            return;
          }

          this.toastService.show(`${successCount} ${pluralSuccessMessage}`, 'success');
        },
        error: (error: unknown) => {
          console.error('Onverwachte fout bij bulkactie', error);
          this.toastService.show('Er is iets misgelopen bij de bulkactie.', 'error');
        },
      });
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