import {
  Component,
  inject,
  signal,
  OnInit,
} from '@angular/core';

import { forkJoin } from 'rxjs';

import {
  Book,
  BookCondition,
  BookCopyCampusLocation,
  BookCopyLocation,
} from '../../../model/book/book.model';
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
  Pencil,
  ChevronDown,
  MapPin,
  Bell,
  BellOff,
} from 'lucide-angular';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { BookService } from '../../../services/bookService/book.service';
import { Availability } from '../../../model/enums/availability.enum';
import { ReviewService } from '../../../services/review-service/review.service';
import { ToastService } from '../../../services/toast/toast.service';
import { ReadingListService } from '../../../services/reading-list/reading-list.service';
import { BookPreview } from '../../../model/book/book-preview.model';
import { RouterLink } from '@angular/router';
import { RoleService } from '../../../services/role-service/role.service';
import { AuthService } from '../../../services/authorization/auth.service';
import { BookLessonTipPreview } from '../../../model/lesson-tip/lesson-tip.model';
import { LESSON_TIP_PATHS } from '../../../routes/lesson-tip.routes';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { BookImagePlaceholderComponent } from '../../shared/book-image-placeholder/book-image-placeholder.component';
import { Campus } from '../../../model/campus.model';
import { CampusService } from '../../../services/campus/campus.service';
import { MailSubscriptionService } from '../../../services/mail-subscription/mail-subscription.service';
import { ReadingList } from '../../../model/readingList.model';
import { BookReviewsComponent } from '../../reviews/book-reviews/book-reviews.component';
import { GamificationNotificationStoreService } from '../../../services/gamification/notification/gamification-notification-store.service';
import {
  CopySelectionModalComponent,
  CopySelectionItem,
} from '../../shared/copy-selection-modal/copy-selection-modal.component';
interface CopyStatusSummary {
  status: 'available' | 'borrowed' | 'lost';
  count: number;
  trackKey: string;
  expectedReturnDate?: string | null;
}

@Component({
  selector: 'app-book-detail',
  standalone: true,
  imports: [
    LucideAngularModule,
    CommonModule,
    FormsModule,
    RouterLink,
    ButtonHoverHelpComponent,
    BookReviewsComponent,
    BookImagePlaceholderComponent,
    CopySelectionModalComponent,
  ],
  templateUrl: './book-detail.component.html',
  styleUrl: './book-detail.component.css',
})
export class BookDetailComponent implements OnInit {
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
  readonly ChevronDown = ChevronDown;
  readonly MapPin = MapPin;
  readonly Bell = Bell;
  readonly BellOff = BellOff;
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
  public readonly starsArray: number[] = [1, 2, 3, 4, 5];
  public averageRating: number = 0;
  public reviewCount: number = 0;

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

  protected readonly isSubscribed = signal(false);
  protected readonly subscriptionLoading = signal(false);
  protected readonly isDeleteModalVisible = signal(false);
  protected readonly isDeletingCopies = signal(false);
  private readonly mailSubscriptionService = inject(MailSubscriptionService);

  Availability = Availability;

  // Related Books Carousel //
  relatedBooksCarousel: Book[] = [];

  protected readonly roleService = inject(RoleService);

  protected readonly erroredCoverImages = new Set<number>();
  protected readonly erroredCarouselImages = new Set<number>();

  protected onCoverImgError(bookId: number | undefined): void {
    if (bookId !== undefined) {
      this.erroredCoverImages.add(bookId);
    }
  }

  protected onCarouselImgError(bookId: number | undefined): void {
    if (bookId !== undefined) {
      this.erroredCarouselImages.add(bookId);
    }
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private readingListService: ReadingListService,
    private reviewService: ReviewService,
    private toastService: ToastService,
    private sanitizer: DomSanitizer,
    protected authService: AuthService,
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
          this.loadSubscriptionStatus(numericBookId);

          if (book.id) {
            this.loadLessonTips(book.id);
          }
        });

        this.loadRelatedBooksCarousel(numericBookId);
        this.updateAverage(numericBookId);
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
    void this.router.navigate([`/${LESSON_TIP_PATHS.root}`, lessonTipId]);
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
  public conditionOptions: { value: BookCondition; label: string }[] = [
    { value: 'GOOD_CONDITION', label: 'Goede staat' },
    { value: 'DAMAGED', label: 'Beschadigd' },
    { value: 'LOST', label: 'Verloren' },
  ];
  selectedBook?: Book;
  selectedCampus?: Campus;

  protected get copyLocationGroups(): BookCopyCampusLocation[] {
    return (
      this.selectedBook?.copyLocations?.filter(
        (group) => group.copies.length > 0,
      ) ?? []
    );
  }

  protected get copySelectionItems(): CopySelectionItem[] {
    const items: CopySelectionItem[] = [];

    for (const group of this.copyLocationGroups) {
      for (const copy of group.copies) {
        items.push({
          id: copy.id,
          label: `Exemplaar #${copy.id}`,
          campusName: group.campusName,
          description:
            this.getCopyAvailabilityLabel(copy) +
            ' · ' +
            this.getConditionLabel(copy.condition),
          isBorrowed: this.isBorrowed(copy.availability),
        });
      }
    }

    return items;
  }

  protected get hasLocationInfo(): boolean {
    return (
      this.copyLocationGroups.length > 0 ||
      !!this.selectedBook?.campusId ||
      !!this.selectedBook?.campusName
    );
  }

  get campusName(): string {
    return (
      this.selectedCampus?.name ??
      this.selectedBook?.campusName ??
      'Onbekende campus'
    );
  }

  get campusAddress(): string | null {
    if (!this.selectedCampus) {
      return null;
    }

    return `${this.selectedCampus.address}, ${this.selectedCampus.postalCode}`;
  }

  protected getCampusAddress(group: BookCopyCampusLocation): string | null {
    if (!group.address) {
      return null;
    }

    return group.postalCode
      ? `${group.address}, ${group.postalCode}`
      : group.address;
  }

  protected getCampusStatusSummaries(
    group: BookCopyCampusLocation,
  ): CopyStatusSummary[] {
    const summaries = new Map<string, CopyStatusSummary>();

    for (const copy of group.copies) {
      const status: CopyStatusSummary['status'] = this.isLostCopy(copy)
        ? 'lost'
        : this.isBorrowed(copy.availability)
          ? 'borrowed'
          : 'available';
      const key =
        status === 'borrowed'
          ? `${status}:${copy.expectedReturnDate ?? ''}`
          : status;
      const existing = summaries.get(key);

      if (existing) {
        existing.count += 1;
        continue;
      }

      summaries.set(key, {
        status,
        count: 1,
        trackKey: key,
        expectedReturnDate:
          status === 'borrowed' ? (copy.expectedReturnDate ?? null) : null,
      });
    }

    return Array.from(summaries.values()).sort((a, b) => {
      if (a.status !== b.status) {
        const order: Record<CopyStatusSummary['status'], number> = {
          available: 0,
          borrowed: 1,
          lost: 2,
        };

        return order[a.status] - order[b.status];
      }

      return (a.expectedReturnDate ?? '').localeCompare(
        b.expectedReturnDate ?? '',
      );
    });
  }

  protected getCopyStatusText(summary: CopyStatusSummary): string {
    const noun = summary.count === 1 ? 'exemplaar' : 'exemplaren';
    const status =
      summary.status === 'available'
        ? 'beschikbaar'
        : summary.status === 'lost'
          ? 'verloren'
          : 'uitgeleend';

    return `${summary.count} ${noun} ${status}`;
  }

  protected getStatusClass(status: CopyStatusSummary['status']): string {
    if (status === 'lost') {
      return 'copy-status--lost';
    }

    return status === 'available'
      ? 'copy-status--available'
      : 'copy-status--borrowed';
  }

  protected getCopyAvailabilityLabel(copy: BookCopyLocation): string {
    if (this.isLostCopy(copy)) {
      return 'Verloren';
    }

    return this.isBorrowed(copy.availability) ? 'Uitgeleend' : 'Beschikbaar';
  }

  protected isLostCopy(copy: BookCopyLocation): boolean {
    return copy.condition === 'LOST';
  }

  protected getConditionLabel(condition?: BookCondition): string {
    switch (condition) {
      case 'DAMAGED':
        return 'Beschadigd';
      case 'LOST':
        return 'Verloren';
      case 'GOOD_CONDITION':
      default:
        return 'Goede staat';
    }
  }

  protected getCopyConditionValue(copy: BookCopyLocation): BookCondition {
    return copy.condition ?? 'GOOD_CONDITION';
  }

  protected updateCopyCondition(
    copy: BookCopyLocation,
    condition: BookCondition,
  ): void {
    this.bookService.updateBookCopyCondition(copy.id, condition).subscribe({
      next: () => {
        copy.condition = condition;
        this.toastService.show(
          'Staat van het exemplaar is bijgewerkt.',
          'success',
        );
      },
      error: () => {
        this.toastService.show(
          'Staat van het exemplaar kon niet worden bijgewerkt.',
          'error',
        );
      },
    });
  }

  private isBorrowed(availability: string | null | undefined): boolean {
    return availability === 'BORROWED' || availability === 'UITGELEEND';
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
      return role === 'LIBRARIAN' || role === 'ADMIN';
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

  getRoundedAverage(): number {
    return Math.round(this.averageRating);
  }

  updateAverage(bookId: number) {
    this.reviewService.getAverageRating(bookId).subscribe({
      next: (avg) => (this.averageRating = avg),
      error: (err) => console.error('Problem with getting average score', err),
    });
  }

  protected onReviewsChanged(): void {
    if (this.selectedBook?.id) {
      this.updateAverage(this.selectedBook.id);
    }
  }

  protected updateReviewCount(reviewCount: number): void {
    this.reviewCount = reviewCount;
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

  public goToEditPage(bookId?: number): void {
    if (!bookId) {
      return;
    }

    this.router.navigate(['/books', bookId, 'edit']);
  }

  public deleteBook(bookId?: number): void {
    this.openDeleteModal();
  }

  protected isLeerling(): boolean {
    return this.authService.currentUser()?.role === 'STUDENT';
  }

  protected openDeleteModal(): void {
    if (this.copySelectionItems.length === 0) {
      this.toastService.show(
        'Er zijn geen exemplaren om te verwijderen.',
        'error',
      );
      return;
    }

    this.isDeleteModalVisible.set(true);
  }

  protected closeDeleteModal(): void {
    this.isDeleteModalVisible.set(false);
    this.isDeletingCopies.set(false);
  }

  protected handleCopyDeletion(selectedIds: number[]): void {
    if (selectedIds.length === 0) {
      return;
    }

    const bookId = this.selectedBook?.id;
    if (!bookId) {
      return;
    }

    this.isDeletingCopies.set(true);

    const allCopyIds = this.copySelectionItems.map((c) => c.id);
    const isDeletingAll = selectedIds.length === allCopyIds.length;

    const deleteObservables = isDeletingAll
      ? [this.bookService.deleteAllBookCopies(bookId)]
      : selectedIds.map((id) => this.bookService.deleteBookCopy(id));

    forkJoin(deleteObservables).subscribe({
      next: () => {
        this.isDeletingCopies.set(false);
        this.isDeleteModalVisible.set(false);
        this.toastService.show(
          selectedIds.length === 1
            ? 'Exemplaar succesvol verwijderd.'
            : `${selectedIds.length} exemplaren succesvol verwijderd.`,
          'success',
        );

        const remainingIds = allCopyIds.filter(
          (id) => !selectedIds.includes(id),
        );

        if (remainingIds.length === 0) {
          void this.router.navigate(['/catalog']);
          return;
        }

        this.bookService.getBookById(bookId).subscribe((book) => {
          this.selectedBook = book;
          this.loadCampusForBook(book);
        });
      },
      error: (err) => {
        this.isDeletingCopies.set(false);
        const backendMessage = err?.error?.error?.message;
        this.toastService.show(
          backendMessage ??
            'Er is een fout opgetreden bij het verwijderen van de exemplaren.',
          'error',
        );
      },
    });
  }

  public goBack(): void {
    void this.router.navigate(['/books']);
  }

  private loadSubscriptionStatus(bookId: number): void {
    this.mailSubscriptionService.getMySubscriptions().subscribe({
      next: (subscriptions) => {
        this.isSubscribed.set(subscriptions.some((s) => s.bookId === bookId));
      },
      error: () => {
        this.isSubscribed.set(false);
      },
    });
  }

  protected toggleSubscription(): void {
    const bookId = this.selectedBook?.id;
    if (!bookId || this.subscriptionLoading()) {
      return;
    }

    this.subscriptionLoading.set(true);

    if (this.isSubscribed()) {
      this.mailSubscriptionService.unsubscribe(bookId).subscribe({
        next: () => {
          this.isSubscribed.set(false);
          this.subscriptionLoading.set(false);
          this.toastService.show(
            'Je ontvangt geen meldingen meer voor dit boek.',
            'success',
          );
        },
        error: () => {
          this.subscriptionLoading.set(false);
          this.toastService.show(
            'Er is een fout opgetreden bij het uitschrijven.',
            'error',
          );
        },
      });
    } else {
      this.mailSubscriptionService.subscribe(bookId).subscribe({
        next: () => {
          this.isSubscribed.set(true);
          this.subscriptionLoading.set(false);
          this.toastService.show(
            'Je ontvangt een melding zodra dit boek beschikbaar is!',
            'success',
          );
        },
        error: (err) => {
          this.subscriptionLoading.set(false);
          const message =
            err?.error?.error?.message ??
            'Er is een fout opgetreden bij het abonneren.';
          this.toastService.show(message, 'error');
        },
      });
    }
  }

  public createLessonTipForCurrentBook(): void {
    const bookId = this.selectedBook?.id;

    if (!bookId) {
      return;
    }

    void this.router.navigate(
      [`/${LESSON_TIP_PATHS.root}/${LESSON_TIP_PATHS.create}`],
      {
        queryParams: { bookId },
      },
    );
  }

}
