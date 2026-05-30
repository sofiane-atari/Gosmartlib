import { CommonModule } from '@angular/common';
import { Component, inject, input, OnDestroy, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChevronDown, Flag, LucideAngularModule, Trash2 } from 'lucide-angular';
import { ReviewDraft } from '../../../model/review/review-draft.model';
import { Review } from '../../../model/review/review.model';
import { PaginatedMeta } from '../../../model/shared/paging/PaginatedMeta.model.ts';
import { AuthService } from '../../../services/authorization/auth.service';
import { GamificationNotificationStoreService } from '../../../services/gamification/notification/gamification-notification-store.service';
import { ReviewService } from '../../../services/review-service/review.service';
import { DraftStorageService } from '../../../services/storage/draft-storage.service';
import { ToastService } from '../../../services/toast/toast.service';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { PagingComponent } from '../../shared/paging/paging.component';

@Component({
  selector: 'app-book-reviews',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LucideAngularModule,
    ButtonHoverHelpComponent,
    PagingComponent,
  ],
  templateUrl: './book-reviews.component.html',
  styleUrl: './book-reviews.component.css',
})
export class BookReviewsComponent implements OnInit, OnDestroy {
  public readonly bookId = input.required<number>();
  public readonly reviewChanged = output<void>();
  public readonly reviewCountChanged = output<number>();

  protected readonly Flag = Flag;
  protected readonly Trash2 = Trash2;
  protected readonly ChevronDown = ChevronDown;
  protected readonly starsArray = [1, 2, 3, 4, 5];

  protected readonly reviews = signal<Review[]>([]);
  protected readonly reviewsLoading = signal(false);
  protected readonly currentPage = signal(0);
  protected readonly pagingMeta = signal<PaginatedMeta>({
    currentPage: 0,
    pageSize: 5,
    totalItems: 0,
    totalPages: 0,
  });

  protected newReviewText = '';
  protected newReviewRating = 5;

  protected readonly authService = inject(AuthService);
  private readonly reviewService = inject(ReviewService);
  private readonly toastService = inject(ToastService);
  private readonly draftStorage = inject(DraftStorageService);
  private readonly gamificationNotificationStore = inject(
    GamificationNotificationStoreService,
  );

  private readonly pageSize = 5;
  private reviewDraftIntervalId: ReturnType<typeof setInterval> | null = null;

  public ngOnInit(): void {
    this.restoreReviewDraft();
    this.startReviewDraftAutosave();
    this.loadReviews(0);
  }

  public ngOnDestroy(): void {
    this.saveReviewDraft();

    if (this.reviewDraftIntervalId) {
      clearInterval(this.reviewDraftIntervalId);
      this.reviewDraftIntervalId = null;
    }
  }

  protected loadReviews(page: number = this.currentPage()): void {
    this.reviewsLoading.set(true);

    this.reviewService.getReviewsForBook(this.bookId(), page, this.pageSize).subscribe({
      next: (response) => {
        this.reviews.set(response.data);
        this.pagingMeta.set(response.meta);
        this.currentPage.set(response.meta.currentPage);
        this.reviewCountChanged.emit(response.meta.totalItems);
        this.reviewsLoading.set(false);
      },
      error: (err) => {
        console.error('Problem with loading the reviews', err);
        this.reviews.set([]);
        this.reviewCountChanged.emit(0);
        this.reviewsLoading.set(false);
        this.toastService.show('De reviews konden niet worden geladen.', 'error');
      },
    });
  }

  protected onPageChange(page: number): void {
    this.loadReviews(page);
  }

  protected setRating(rating: number): void {
    this.newReviewRating = rating;
    this.saveReviewDraft();
  }

  protected submitReview(): void {
    if (this.isLeerling() && this.currentUserAlreadyReviewed()) {
      this.toastService.show(
        'Je kunt maar één review per boek schrijven.',
        'error',
      );
      return;
    }

    if (!this.newReviewText.trim()) {
      return;
    }

    const newReview: Review = {
      comment: this.newReviewText,
      rating: this.newReviewRating,
    };

    this.reviewService.addReviewToBook(this.bookId(), newReview).subscribe({
      next: () => {
        this.newReviewText = '';
        this.newReviewRating = 5;
        this.clearReviewDraft();
        this.currentPage.set(0);
        this.loadReviews(0);
        this.reviewChanged.emit();
        this.toastService.show('Review succesvol geplaatst!', 'success');
        this.gamificationNotificationStore.loadUnreadNotifications();
      },
      error: (err) => {
        console.error('Problem with storing review', err);

        const message =
          err?.error?.message ??
          err?.error?.error?.message ??
          'Je review kon niet geplaatst worden.';

        this.toastService.show(message, 'error');
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
            this.reloadAfterReviewMutation();
            this.reviewChanged.emit();
            this.toastService.show('Review verwijderd!', 'success');
          },
          error: () => this.toastService.show('De review kon niet worden verwijderd.', 'error'),
        });
      },
      () => {},
    );
  }

  protected reportReview(review: Review): void {
    if (!review.id || review.reportedByCurrentUser) {
      return;
    }

    this.toastService.showConfirm(
      'Weet je zeker dat je deze review wilt rapporteren? Onterecht rapporteren kan gevolgen hebben.',
      () => {
        this.reviewService.reportReview(review.id!).subscribe({
          next: () => {
            this.reloadAfterReviewMutation();
            this.reviewChanged.emit();
            this.toastService.show('Review succesvol gerapporteerd.', 'success');
          },
          error: (err) => {
            console.error('Problem with reporting review', err);
            this.toastService.show(
              'Er liep iets mis bij het rapporteren van deze review.',
              'error',
            );
          },
        });
      },
      () => {},
    );
  }

  protected currentUserAlreadyReviewed(): boolean {
    return this.reviews().some((review) => review.currentUserReview === true);
  }

  private isLeerling(): boolean {
    return this.authService.currentUser()?.role === 'STUDENT';
  }

  private reloadAfterReviewMutation(): void {
    const fallbackPage = Math.max(this.currentPage() - 1, 0);
    const shouldFallbackToPreviousPage =
      this.reviews().length === 1 && this.currentPage() > 0;

    this.loadReviews(shouldFallbackToPreviousPage ? fallbackPage : this.currentPage());
  }

  private getReviewDraftKey(): string {
    return `book-review-${this.bookId()}`;
  }

  private saveReviewDraft(): void {
    const draft: ReviewDraft = {
      text: this.newReviewText,
      rating: this.newReviewRating,
    };

    this.draftStorage.saveDraft(this.getReviewDraftKey(), draft);
  }

  private restoreReviewDraft(): void {
    const draft = this.draftStorage.getDraft<ReviewDraft>(this.getReviewDraftKey());

    if (!draft) {
      return;
    }

    this.newReviewText = draft.text ?? '';
    this.newReviewRating = draft.rating ?? 5;
  }

  private clearReviewDraft(): void {
    this.draftStorage.removeDraft(this.getReviewDraftKey());
  }

  private startReviewDraftAutosave(): void {
    if (this.reviewDraftIntervalId) {
      clearInterval(this.reviewDraftIntervalId);
    }

    this.reviewDraftIntervalId = setInterval(() => {
      this.saveReviewDraft();
    }, 1000);
  }
}
