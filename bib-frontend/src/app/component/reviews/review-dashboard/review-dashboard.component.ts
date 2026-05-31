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
  Eraser,
} from 'lucide-angular';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';

import { Review, ReviewDashboardStats } from '../../../model/review/review.model';
import { AuthUserDto } from '../../../model/auth.model';
import { ReviewService } from '../../../services/review-service/review.service';
import { ToastService } from '../../../services/toast/toast.service';
import { PagingMeta, PagingComponent } from '../../shared/paging/paging.component';
import {
  reviewCardAnimation,
  fadeInUpAnimation,
} from '../../shared/animations/my-reviews.animations';
import { UserService } from '../../../services/user/user.service';
import { RoleService } from '../../../services/role-service/role.service';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { StatCardComponent } from "../../shared/stat-card/stat-card.component";
import { ReviewBlockedListComponent } from '../review-blocked-list/review-blocked-list.component';

@Component({
  selector: 'app-review-dashboard',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, PagingComponent, ButtonHoverHelpComponent, StatCardComponent, ReviewBlockedListComponent],
  templateUrl: './review-dashboard.component.html',
  styleUrl: './review-dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [reviewCardAnimation, fadeInUpAnimation],
})
export class ReviewDashboardComponent {
  private readonly reviewService = inject(ReviewService);
  private readonly toastService = inject(ToastService);
  private readonly userService = inject(UserService);
  protected readonly roleService = inject(RoleService);

  protected readonly Check = Check;
  protected readonly Trash2 = Trash2;
  protected readonly Flag = Flag;
  protected readonly CircleAlert = CircleAlert;
  protected readonly Star = Star;
  protected readonly eraser = Eraser;

  protected readonly showBlockedUsers = signal<boolean>(false);
  protected readonly blockedUsers = signal<AuthUserDto[]>([]);
  protected readonly isLoadingBlocked = signal<boolean>(false);
  protected readonly blockedError = signal<string | null>(null);

  protected readonly reviews = signal<Review[]>([]);
  protected readonly selectedReviewIds = signal<Set<number>>(new Set());
  protected readonly isLoading = signal<boolean>(false);
  protected readonly isBulkProcessing = signal<boolean>(false);
  protected readonly pagingMeta = signal<PagingMeta | null>(null);

  private readonly currentPage = signal<number>(0);
  private readonly pageSize = signal<number>(10);
  protected readonly stats = signal<ReviewDashboardStats | null>(null);

  protected readonly selectedCount = computed(
    () => this.selectedReviewIds().size,
  );

  protected readonly allVisibleReviewsSelected = computed(() => {
    const visibleReviewIds = this.reviews()
      .map((review) => review.id)
      .filter((id): id is number => id !== undefined);

    return (
      visibleReviewIds.length > 0 &&
      visibleReviewIds.every((reviewId) => this.selectedReviewIds().has(reviewId))
    );
  });

  protected readonly criticalReviewsCount = computed(
    () => this.stats()?.criticalFlaggedReviews ?? 0,
  );

  protected readonly totalCount = computed(
    () => this.stats()?.totalFlaggedReviews ?? this.pagingMeta()?.totalItems ?? 0,
  );

  protected readonly starValues = [1, 2, 3, 4, 5];
  protected readonly skeletonItems = [1, 2, 3, 4, 5];

  public ngOnInit(): void {
    this.loadFlaggedReviews(this.currentPage());
    this.loadFlaggedReviewStats();
  }

  protected onPageChange(page: number): void {
    this.currentPage.set(page);
    this.selectedReviewIds.set(new Set());
    this.loadFlaggedReviews(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
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
            this.applyProcessedReviewIds([reviewId]);
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
            this.applyProcessedReviewIds([reviewId]);
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

  // New: block a reviewer from posting reviews
  protected blockReviewer(review: Review): void {
    if (!review.reviewerId) {
      return;
    }

    this.toastService.showConfirm(
      'Weet je zeker dat je deze leerling wilt blokkeren voor het plaatsen van reviews?',
      () => {
        this.userService.updateReviewAccess(review.reviewerId!, true).subscribe({
          next: (updatedUser) => {
            // update local review state to reflect reviewer is blocked
            this.reviews.update((list) =>
              list.map((r) =>
                r.reviewerId === review.reviewerId
                  ? { ...r, reviewerReviewBlocked: true }
                  : r,
              ),
            );
            this.toastService.show('Leerling geblokkeerd voor reviews.', 'success');
          },
          error: (error) => {
            console.error('Fout bij blokkeren leerling', error);
            this.toastService.show('De leerling kon niet geblokkeerd worden.', 'error');
          },
        });
      },
      () => {},
    );
  }

  // New: unblock a reviewer
  protected unblockReviewer(review: Review): void {
    if (!review.reviewerId) {
      return;
    }

    this.toastService.showConfirm(
      'Weet je zeker dat je deze leerling wilt deblokkeren voor het plaatsen van reviews?',
      () => {
        this.userService.updateReviewAccess(review.reviewerId!, false).subscribe({
          next: (updatedUser) => {
            this.reviews.update((list) =>
              list.map((r) =>
                r.reviewerId === review.reviewerId
                  ? { ...r, reviewerReviewBlocked: false }
                  : r,
              ),
            );
            this.toastService.show('Leerling gedeblokkeerd voor reviews.', 'success');
          },
          error: (error) => {
            console.error('Fout bij deblokkeren leerling', error);
            this.toastService.show('De leerling kon niet gedeblokkeerd worden.', 'error');
          },
        });
      },
      () => {},
    );
  }

  protected openBlockedUsersModal(): void {
    this.showBlockedUsers.set(true);
    this.isLoadingBlocked.set(true);
    this.blockedError.set(null);

    this.userService.getReviewBlockedUsers().subscribe({
      next: (users) => {
        this.blockedUsers.set(users);
        this.isLoadingBlocked.set(false);
      },
      error: () => {
        this.blockedError.set('De geblokkeerde leerlingen konden niet opgehaald worden.');
        this.isLoadingBlocked.set(false);
      },
    });
  }

  protected onUnblockFromModal(userId: string): void {
    this.userService.updateReviewAccess(userId, false).subscribe({
      next: () => {
        this.blockedUsers.update((users) => users.filter((u) => u.id !== userId));
        this.reviews.update((list) =>
          list.map((r) =>
            r.reviewerId === userId
              ? { ...r, reviewerReviewBlocked: false }
              : r,
          ),
        );
        this.toastService.show('Leerling gedeblokkeerd voor reviews.', 'success');
      },
      error: () => {
        this.toastService.show('De leerling kon niet gedeblokkeerd worden.', 'error');
      },
    });
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

  protected selectAllVisibleReviews(): void {
    if (this.isBulkProcessing()) {
      return;
    }

    const visibleReviewIds = this.reviews()
      .map((review) => review.id)
      .filter((id): id is number => id !== undefined);

    this.selectedReviewIds.set(new Set(visibleReviewIds));
  }
  
  protected deselectAll(): void {
    this.selectedReviewIds.set(new Set());
  }

  private loadFlaggedReviewStats(): void {
  this.reviewService.getFlaggedReviewStats().subscribe({
    next: (stats) => {
      this.stats.set(stats);
    },
    error: (error) => {
      console.error('Fout bij ophalen van flagged review stats', error);
    },
  });
}

  private loadFlaggedReviews(page: number): void {
    this.isLoading.set(true);

    this.reviewService.getFlaggedReviews(page, this.pageSize()).subscribe({
      next: (response) => {
        this.reviews.set(response.data);
        this.pagingMeta.set(response.meta);
        this.currentPage.set(response.meta.currentPage);
        this.selectedReviewIds.set(new Set());
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
      .pipe(finalize(() => this.isBulkProcessing.set(false)))
      .subscribe({
        next: (results) => {
          const successfulReviewIds = results
            .filter((result) => result.success)
            .map((result) => result.reviewId);

          this.applyProcessedReviewIds(successfulReviewIds);

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

  private applyProcessedReviewIds(processedReviewIds: number[]): void {
    if (processedReviewIds.length === 0) {
      return;
    }

    const currentReviews = this.reviews();
    const processedIdsSet = new Set(processedReviewIds);

    const processedReviews = currentReviews.filter((review) =>
      processedIdsSet.has(review.id ?? -1),
    );

    const removedCriticalCount = processedReviews.filter(
      (review) => (review.reportCount ?? 0) >= 5,
    ).length;

    this.stats.update((currentStats) => {
      if (!currentStats) {
        return currentStats;
      }

      return {
        totalFlaggedReviews: Math.max(
          currentStats.totalFlaggedReviews - processedReviewIds.length,
          0,
        ),
        criticalFlaggedReviews: Math.max(
          currentStats.criticalFlaggedReviews - removedCriticalCount,
          0,
        ),
      };
    });

    this.reviews.update((reviews) =>
      reviews.filter((review) => !processedIdsSet.has(review.id ?? -1)),
    );

    const nextSelectedReviewIds = new Set(this.selectedReviewIds());
    processedReviewIds.forEach((reviewId: number) => {
      nextSelectedReviewIds.delete(reviewId);
    });
    this.selectedReviewIds.set(nextSelectedReviewIds);

    const currentMeta = this.pagingMeta();

    if (!currentMeta) {
      return;
    }

    const updatedTotalItems = Math.max(
      currentMeta.totalItems - processedReviewIds.length,
      0,
    );

    const updatedTotalPages =
      updatedTotalItems === 0
        ? 0
        : Math.ceil(updatedTotalItems / currentMeta.pageSize);

    const nextPageToLoad = Math.min(
      currentMeta.currentPage,
      Math.max(updatedTotalPages - 1, 0),
    );

    this.pagingMeta.set({
      ...currentMeta,
      totalItems: updatedTotalItems,
      totalPages: updatedTotalPages,
      currentPage: nextPageToLoad,
    });

    const shouldReload =
      this.reviews().length === 0 && updatedTotalItems > 0;

    if (shouldReload) {
      this.loadFlaggedReviews(nextPageToLoad);
      return;
    }

    this.currentPage.set(nextPageToLoad);
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
