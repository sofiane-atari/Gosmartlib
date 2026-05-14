import {
  Component,
  Inject,
  inject,
  OnDestroy,
  signal,
  OnInit,
} from '@angular/core';

import { Book } from '../../../model/book/book.model';
import {
  LucideAngularModule,
  BookOpen,
  X,
  Library,
  Minus,
  Plus,
  CheckCircle,
  Trash2,
  Calendar,
  Star,
  MessageCircle,
  Bookmark,
  Flag,
  Pencil,
  ChevronDown,
  MapPin,
} from 'lucide-angular';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { BookService } from '../../../services/bookService/book.service';
import { Availability } from '../../../model/enums/availability.enum';
import { Review } from '../../../model/review/review.model';
import { ReviewService } from '../../../services/review-service/review.service';
import { ToastService } from '../../../services/toast/toast.service';
import {
  ReadingListService,
  ReadingList,
} from '../../../services/reading-list/reading-list.service';
import { BookPreview } from '../../../model/book/book-preview.model';
import { RouterLink } from '@angular/router';
import { RoleService } from '../../../services/role-service/role.service';
import { AuthService } from '../../../services/authorization/auth.service';
import { BookLessonTipPreview } from '../../../model/lesson-tip/lesson-tip.model';
import { LESSON_TIP_PATHS } from '../../../routes/lesson-tip.routes';
import { DraftStorageService } from '../../../services/storage/draft-storage.service';
import { ReviewDraft } from '../../../model/review/review-draft.model';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { Campus } from '../../../model/campus.model';
import { CampusService } from '../../../services/campus/campus.service';

@Component({
  selector: 'app-book-detail',
  standalone: true,
  imports: [
    LucideAngularModule,
    CommonModule,
    FormsModule,
    RouterLink,
    ButtonHoverHelpComponent,
  ],
  templateUrl: './book-detail.component.html',
  styleUrl: './book-detail.component.css',
})
export class BookDetailComponent implements OnDestroy, OnInit {
  readonly X = X;
  readonly Library = Library;
  readonly Minus = Minus;
  readonly Plus = Plus;
  readonly CheckCircle = CheckCircle;
  readonly Pencil = Pencil;
  readonly Trash2 = Trash2;
  readonly Calendar = Calendar;
  readonly Star = Star;
  readonly MessageCircle = MessageCircle;
  readonly BookOpen = BookOpen;
  readonly Bookmark = Bookmark;
  readonly Flag = Flag;
  readonly ChevronDown = ChevronDown;
  readonly MapPin = MapPin;
  icons = [
    { icon: Library, label: 'Bibliotheek' },
    { icon: Star, label: 'Reviews' },
    { icon: MessageCircle, label: 'Berichten' },
    { icon: Calendar, label: 'Planning' },
    { icon: CheckCircle, label: 'Voltooid' },
    { icon: Pencil, label: 'Bewerken' },
    { icon: Plus, label: 'Toevoegen' },
    { icon: Minus, label: 'Verwijderen' },
    { icon: Trash2, label: 'Prullenbak' },
    { icon: X, label: 'Sluiten' },
  ];

  //Reviews//
  public reviews: Review[] = [];
  public newReviewText: string = '';
  public newReviewRating: number = 5;
  public readonly starsArray: number[] = [1, 2, 3, 4, 5];
  public averageRating: number = 0;

  // Lists //
  readingLists: ReadingList[] = [];
  pickerReadingLists: ReadingList[] = [];
  showListPicker: boolean = false;

  // Preview //
  public readonly previewState = signal<'idle' | 'loading' | 'ready' | 'error'>(
    'idle',
  );
  public readonly previewData = signal<BookPreview | null>(null);
  public readonly previewErrorMessage = signal<string>('');
  public readonly embeddedPreviewUrl = signal<SafeResourceUrl | null>(null);
  public readonly previewFallbackUrl = signal<string | null>(null);
  public readonly isPreviewModalOpen = signal<boolean>(false);

  protected readonly lessonTipPreview = signal<BookLessonTipPreview | null>(
    null,
  );
  protected readonly isLoadingLessonTips = signal(false);

  Availability = Availability;

  // Related Books Carousel //
  relatedBooksCarousel: Book[] = [];

  private currentBookId: number | null = null;
  private reviewDraftIntervalId: ReturnType<typeof setInterval> | null = null;

  protected readonly roleService = inject(RoleService);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private readingListService: ReadingListService,
    private reviewService: ReviewService,
    private toastService: ToastService,
    private sanitizer: DomSanitizer,
    protected authService: AuthService,
    private draftStorage: DraftStorageService,
    private campusService: CampusService,
  ) {}

  public ngOnInit(): void {
    // Ensure current user role is available for picker filtering.
    this.authService.fetchCurrentUser();

    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');

      if (id) {
        const numericBookId = +id;

        this.bookService.getBookById(numericBookId).subscribe((book) => {
          this.selectedBook = book;
          this.loadCampusForBook(book);

          if (book.id) {
            this.loadLessonTips(book.id);
          }
        });

        this.currentBookId = numericBookId;
        this.restoreReviewDraft(numericBookId);
        this.startReviewDraftAutosave();

        this.loadRelatedBooksCarousel(numericBookId);
        this.loadReviews(numericBookId);
      }
    });

    this.loadReadingLists();
  }

  private loadRelatedBooksCarousel(bookId: number) {
    this.bookService.getRelatedBooksCarousel(bookId, 10).subscribe({
      next: (books) => {
        this.relatedBooksCarousel = books;
      },
      error: (err) => {
        console.error('Problem with loading carousel books', err);
        this.relatedBooksCarousel = [];
      },
    });
  }

  private loadLessonTips(bookId: number): void {
    this.isLoadingLessonTips.set(true);

    this.bookService.getLessonTipPreviewByBookId(bookId).subscribe({
      next: (preview) => {
        this.lessonTipPreview.set(preview);
        this.isLoadingLessonTips.set(false);
      },
      error: () => {
        this.lessonTipPreview.set(null);
        this.isLoadingLessonTips.set(false);
        this.toastService.show(
          'De lestips bij dit boek konden niet worden geladen.',
          'error',
        );
      },
    });
  }

  public openLessonTipDetail(lessonTipId: number): void {
    void this.router.navigate(['/lesson', lessonTipId]);
  }

  public openAllLessonTipsForBook(): void {
    const bookId = this.selectedBook?.id;

    if (!bookId) {
      return;
    }

    void this.router.navigate([`/${LESSON_TIP_PATHS.root}`], {
      queryParams: { bookId },
    });
  }

  scrollCarousel(direction: 'left' | 'right') {
    const carousel = document.querySelector(
      '.related-books-carousel-container',
    );
    if (!carousel) return;

    const scrollAmount = 240; // 90px kaart + 16px gap + wat padding
    if (direction === 'left') {
      carousel.scrollLeft -= scrollAmount;
    } else {
      carousel.scrollLeft += scrollAmount;
    }
  }

  public availabilityOptions = ['AVAILABLE', 'DAMAGED', 'LOST', 'BORROWED'];
  selectedBook?: Book;
  selectedCampus?: Campus;

  get campusName(): string {
    return this.selectedCampus?.name ?? this.selectedBook?.campusName ?? 'Onbekende campus';
  }

  get campusAddress(): string | null {
    if (!this.selectedCampus) {
      return null;
    }

    return `${this.selectedCampus.address}, ${this.selectedCampus.postalCode}`;
  }

  private loadCampusForBook(book: Book): void {
    this.selectedCampus = undefined;

    if (!book.campusId) {
      return;
    }

    this.campusService.getById(book.campusId).subscribe({
      next: (campus) => {
        this.selectedCampus = campus;
      },
      error: (error) => {
        console.error('Problem with loading campus details', error);
      },
    });
  }

  loadReviews(bookId: number) {
    this.reviewService.getReviewsForBook(bookId).subscribe({
      next: (data) => {
        this.reviews = data.filter(
          (review) => review.moderationStatus !== 'FLAGGED',
        );
      },
      error: (err) => console.error('Problem with loading the reviews', err),
    });

    this.updateAverage(bookId);
  }

  loadReadingLists() {
    this.readingListService.getLists().subscribe((lists) => {
      this.readingLists = lists;
      this.pickerReadingLists = lists.filter((list) =>
        this.canShowInPicker(list),
      );
    });
  }

  private canShowInPicker(list: ReadingList): boolean {
    const target = list.targetType ?? 'PERSONAL';
    const role = this.authService.currentUser()?.role;

    if (target === 'SYSTEM') {
      return false;
    }

    if (target === 'PERSONAL') {
      return true;
    }

    return role === 'TEACHER';
  }

  toggleListPicker() {
    this.showListPicker = !this.showListPicker;
  }

  addBookToList(listId: number) {
    if (this.selectedBook?.id) {
      this.readingListService
        .addBookToList(listId, this.selectedBook.id)
        .subscribe({
          next: () => {
            this.toastService.show(`Boek toegevoegd aan leeslijst!`, 'success');
            this.showListPicker = false;
          },
          error: (err: any) => {
            if (err.error === 'Boek zit al in deze leeslijst') {
              this.toastService.show(
                'Dit boek staat al in deze leeslijst!',
                'error',
              );
            } else {
              console.error('Fout bij toevoegen aan leeslijst', err);
              this.toastService.show(
                'Er is een fout opgetreden bij het toevoegen aan de leeslijst.',
                'error',
              );
            }
          },
        });
    }
  }

  setRating(rating: number) {
    this.newReviewRating = rating;
    this.saveReviewDraft();
  }

  getRoundedAverage(): number {
    return Math.round(this.averageRating);
  }

  updateAverage(bookId: number) {
    this.reviewService.getAverageRating(bookId).subscribe({
      next: (avg) => (this.averageRating = avg),
      error: (err) => console.error('Problem with getting average score', err),
    });
  }

  submitReview() {
    const bookId = this.selectedBook?.id;

    if (this.isLeerling() && this.currentUserAlreadyReviewed()) {
      this.toastService.show(
        'Je kunt maar één review per boek schrijven.',
        'error',
      );
      return;
    }

    if (!this.newReviewText.trim() || !bookId) return;

    const newReview: Review = {
      comment: this.newReviewText,
      rating: this.newReviewRating,
    };

    this.reviewService.addReviewToBook(bookId, newReview).subscribe({
      next: (savedReview) => {
        this.reviews.push(savedReview);
        this.newReviewText = '';
        this.newReviewRating = 5;
        this.clearReviewDraft();

        this.updateAverage(bookId);
        this.toastService.show('Review succesvol geplaatst!', 'success');
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

  deleteReview(reviewId?: number) {
    if (!reviewId) return;

    this.toastService.showConfirm(
      'Weet je zeker dat je deze review wilt verwijderen?',
      () => {
        this.reviewService.deleteReview(reviewId).subscribe({
          next: () => {
            this.reviews = this.reviews.filter((r) => r.id !== reviewId);
            if (this.selectedBook?.id) this.updateAverage(this.selectedBook.id);
            this.toastService.show('Review verwijderd!', 'success');
          },
          error: () =>
            this.toastService.show('Problem with deleting!', 'error'),
        });
      },
      () => {},
    );
  }

  reportReview(review: Review) {
    if (!review.id || review.reportedByCurrentUser) return;

    this.toastService.showConfirm(
      'Weet je zeker dat je deze review wilt rapporteren? Onterecht rapporteren kan gevolgen hebben.',
      () => {
        this.reviewService.reportReview(review.id!).subscribe({
          next: () => {
            review.reportedByCurrentUser = true;
            review.reportCount = (review.reportCount || 0) + 1;

            this.reviews = this.reviews.filter((r) => r.id !== review.id);

            if (this.selectedBook?.id) {
              this.updateAverage(this.selectedBook.id);
            }

            this.toastService.show(
              'Review succesvol gerapporteerd.',
              'success',
            );
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

  public loadPreview(): void {
    const isbn = this.getSelectedBookIsbn();

    this.isPreviewModalOpen.set(true);

    if (!isbn) {
      this.previewState.set('error');
      this.previewErrorMessage.set(
        'Geen geldig ISBN beschikbaar voor dit boek.',
      );
      this.previewData.set(null);
      this.embeddedPreviewUrl.set(null);
      this.previewFallbackUrl.set(null);
      return;
    }

    this.previewState.set('loading');
    this.previewErrorMessage.set('');
    this.embeddedPreviewUrl.set(null);
    this.previewFallbackUrl.set(null);

    this.bookService.getBookPreviewByISBN(isbn).subscribe({
      next: (preview) => {
        this.previewData.set(preview);

        const safeFallback = this.isSafeExternalUrl(preview.detailsUrl)
          ? preview.detailsUrl
          : null;

        const safeEmbed = this.toSafeResourceUrl(preview.embedUrl);
        if (safeEmbed) {
          this.embeddedPreviewUrl.set(safeEmbed);
          this.previewFallbackUrl.set(safeFallback);
          this.previewState.set('ready');
          return;
        }

        const safePreview = this.isSafeExternalUrl(preview.previewUrl)
          ? preview.previewUrl
          : null;

        if (safePreview) {
          this.previewFallbackUrl.set(safePreview);
          this.previewState.set('ready');
          return;
        }

        if (safeFallback) {
          this.previewFallbackUrl.set(safeFallback);
          this.previewState.set('ready');
          return;
        }

        this.previewState.set('error');
        this.previewErrorMessage.set(
          'Er is geen veilige preview-link beschikbaar.',
        );
      },
      error: (err) => {
        console.error('Problem with loading preview', err);
        this.previewData.set(null);
        this.embeddedPreviewUrl.set(null);
        this.previewFallbackUrl.set(null);
        this.previewState.set('error');
        const backendMessage =
          typeof err?.error?.message === 'string' ? err.error.message : null;
        this.previewErrorMessage.set(
          backendMessage ||
            'Preview kon niet geladen worden. Probeer later opnieuw.',
        );
      },
    });
  }

  private getSelectedBookIsbn(): string | null {
    const normalizedBook = this.selectedBook as Book & { isbn?: string };
    const isbn =
      this.selectedBook?.isbn?.trim() || normalizedBook?.isbn?.trim();
    return isbn && isbn.length > 0 ? isbn : null;
  }

  private isSafeExternalUrl(url: string | null | undefined): url is string {
    if (!url) {
      return false;
    }

    try {
      const parsed = new URL(url);
      const isHttps = parsed.protocol === 'https:';
      const host = parsed.hostname.toLowerCase();
      const allowedHost =
        host === 'openlibrary.org' ||
        host.endsWith('.openlibrary.org') ||
        host === 'archive.org' ||
        host.endsWith('.archive.org');

      return isHttps && allowedHost;
    } catch {
      return false;
    }
  }

  private toSafeResourceUrl(
    url: string | null | undefined,
  ): SafeResourceUrl | null {
    if (!this.isSafeExternalUrl(url)) {
      return null;
    }

    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  public closePreviewModal(): void {
    this.isPreviewModalOpen.set(false);
    this.embeddedPreviewUrl.set(null);
  }

  public openRelatedBook(bookId?: number): void {
    if (!bookId) {
      return;
    }

    this.router.navigate(['/book', bookId]);
  }

  protected currentUserAlreadyReviewed(): boolean {
    return this.reviews.some((review) => review.currentUserReview === true);
  }

  public goToEditPage(bookId?: number): void {
    if (!bookId) {
      return;
    }

    this.router.navigate(['/books', bookId, 'edit']);
  }

  protected isLeerling(): boolean {
    return this.authService.currentUser()?.role === 'STUDENT';
  }

  public deleteBook(bookId?: number): void {
    if (!bookId) {
      return;
    }

    this.toastService.showConfirm(
      'Weet je zeker dat je dit boek wilt verwijderen?',
      () => {
        this.bookService.deleteBook(bookId).subscribe({
          next: () => {
            this.toastService.show('Boek succesvol verwijderd.', 'success');
            void this.router.navigate(['/catalog']);
          },
          error: (err) => {
            const backendMessage = err?.error?.error?.message;
            this.toastService.show(
              backendMessage ?? 'Het boek kon niet verwijderd worden.',
              'error',
            );
          },
        });
      },
      () => {},
    );
  }

  private getReviewDraftKey(bookId: number): string {
    return `book-review-${bookId}`;
  }

  private saveReviewDraft(): void {
    if (!this.currentBookId) {
      return;
    }

    const draft: ReviewDraft = {
      text: this.newReviewText,
      rating: this.newReviewRating,
    };

    this.draftStorage.saveDraft(
      this.getReviewDraftKey(this.currentBookId),
      draft,
    );
  }

  private restoreReviewDraft(bookId: number): void {
    const draft = this.draftStorage.getDraft<ReviewDraft>(
      this.getReviewDraftKey(bookId),
    );

    if (!draft) {
      return;
    }

    this.newReviewText = draft.text ?? '';
    this.newReviewRating = draft.rating ?? 5;
  }

  private clearReviewDraft(): void {
    if (!this.currentBookId) {
      return;
    }

    this.draftStorage.removeDraft(this.getReviewDraftKey(this.currentBookId));
  }

  private startReviewDraftAutosave(): void {
    if (this.reviewDraftIntervalId) {
      return;
    }

    this.reviewDraftIntervalId = setInterval(() => {
      this.saveReviewDraft();
    }, 1000);
  }

  ngOnDestroy(): void {
    this.saveReviewDraft();

    if (this.reviewDraftIntervalId) {
      clearInterval(this.reviewDraftIntervalId);
      this.reviewDraftIntervalId = null;
    }
  }
}
