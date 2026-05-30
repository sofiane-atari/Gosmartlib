import {
  Component,
  computed,
  OnInit,
  signal,
  inject,
  effect,
  DestroyRef,
  HostListener,
} from '@angular/core';
import { Book } from '../../../model/book/book.model';
import { BookService } from '../../../services/bookService/book.service';
import { ReadingListService } from '../../../services/reading-list/reading-list.service';
import { ToastService } from '../../../services/toast/toast.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { BookFilters } from '../../../model/book/bookFilter.model';
import { combineLatest, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  LucideAngularModule,
  Heart,
  Star,
  BookPlus,
  Search,
  Locate,
  ChevronDown,
} from 'lucide-angular';
import { BookListSidebarComponent } from '../book-list-sidebar/book-list-sidebar.component';
import { BookImagePlaceholderComponent } from '../../shared/book-image-placeholder/book-image-placeholder.component';
import { PagingComponent } from '../../shared/paging/paging.component';
import { PaginatedResponse } from '../../../model/shared/paging/PaginatedResponse.model';
import { ReadingLevel } from '../../../model/readingLevel.model';
import { ReadingLevelsService } from '../../../services/readingLevels/reading-levels.service';
import { Genre } from '../../../model/genre/genre.model';
import { GenreService } from '../../../services/genre/genre.service';
import { GenrePickerComponent } from '../../shared/genre-picker/genre-picker/genre-picker.component';
import { CampusService } from '../../../services/campus/campus.service';
import { ReadingList } from '../../../model/readingList.model';
import { FeaturedReadingListsSidebarComponent } from "../featured-reading-lists-sidebar/featured-reading-lists-sidebar.component";
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';
import { UserSettingsService } from '../../../services/user-settings/user-settings.service';
import { SearchBarComponent } from "../../shared/search-bar/search-bar.component";
import { RoleService } from '../../../services/role-service/role.service';
import { School } from '../../../model/school.model';
import { SchoolService } from '../../../services/school/school.service';
import { SelectedSchoolService } from '../../../services/selected-school/selected-school.service';
import { AuthService } from '../../../services/authorization/auth.service';

@Component({
  selector: 'app-books',
  standalone: true,
  imports: [
    FormsModule,
    CommonModule,
    LucideAngularModule,
    BookListSidebarComponent,
    BookImagePlaceholderComponent,
    PagingComponent,
    GenrePickerComponent,
    FeaturedReadingListsSidebarComponent,
    SearchBarComponent
],
  templateUrl: './books.component.html',
  styleUrl: './books.component.css',
  animations: [fadeInUpAnimation],
})
export class BooksComponent implements OnInit {
  readonly Book = BookPlus;
  readonly Heart = Heart;
  readonly Search = Search;
  readonly Star = Star;
  readonly Locate = Locate;
  readonly ChevronDown = ChevronDown;

  private readonly bookService = inject(BookService);
  private readonly router = inject(Router);
  private readonly readingListService = inject(ReadingListService);
  private readonly readingLevelsService = inject(ReadingLevelsService);
  private readonly genreService = inject(GenreService);
  private readonly campusService = inject(CampusService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly roleService = inject(RoleService);
  private readonly schoolService = inject(SchoolService);
  private readonly selectedSchoolService = inject(SelectedSchoolService);
  protected readonly authService = inject(AuthService);

  books = signal<Book[]>([]);
  protected readonly pagingMeta = signal({
    currentPage: 0,
    pageSize: 10,
    totalItems: 0,
    totalPages: 0,
  });

  

  protected readonly catalogLoading = signal(false);

  protected readonly mostReadBooks = signal<Book[]>([]);
  protected readonly newestBooks = signal<Book[]>([]);
  protected readonly mostReadLoading = signal(true);
  protected readonly newestLoading = signal(true);
  private readonly searchSubject = new Subject<string>();
  private readonly pageRangeSubject = new Subject<void>();
  protected readonly searchInput = signal('');

  protected readonly absoluteMaxPages = signal(0);
  protected readonly absoluteMinPages = signal(0);
  protected readonly readingLevels = signal<ReadingLevel[]>([]);
  protected readonly genres = signal<Genre[]>([]);
  protected readonly hasMultipleCampuses = signal(false);

  protected readonly currentPage = signal(0);
  private readonly pageSize = signal(15);
  protected readonly userSettingsService = inject(UserSettingsService);
  protected readonly canFilterDidacticBooks = computed(() =>
    this.roleService.canFilterDidacticBooks()
  );

  protected readonly canViewOtherSchools = computed(() =>
    this.roleService.canViewOtherSchools()
  );

  protected readonly accessibleSchools = signal<School[]>([]);
  protected readonly schoolsLoading = signal(false);

  protected readonly selectedSchoolId = this.selectedSchoolService.schoolId;
  protected readonly currentSchoolName = this.selectedSchoolService.schoolName;
  protected readonly isSchoolDropdownOpen = signal(false);

  private readonly favoritesListId = signal<number | null>(null);

  protected readonly erroredImages = new Set<number>();

  featuredReadingLists: ReadingList[] = [];
  isLoadingFeaturedReadingLists = false;

  constructor() {
    effect(() => {
      localStorage.setItem('bookFilters', JSON.stringify(this.filters()));
    });

    effect(() => {
      const status = this.authService.adminOverrideStatus();
      const user = this.authService.currentUser();

      if (!user?.schoolId) {
        return;
      }

      if (this.canViewOtherSchools()) {
        return;
      }

      if (status?.enabled) {
        return;
      }

      if (Number(this.selectedSchoolId()) === Number(user.schoolId)) {
        return;
      }

      this.selectedSchoolService.selectSchool(user.schoolId, user.schoolName ?? '');
      this.currentPage.set(0);
      this.loadCatalogBooks();
      this.loadSidebarBooks();
      this.loadFeaturedReadingLists();
    }, { allowSignalWrites: true });

    effect(() => {
      if (this.canViewOtherSchools()) {
        this.loadAccessibleSchools();
      }
    }, { allowSignalWrites: true });

    this.pageRangeSubject
      .pipe(debounceTime(400), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.currentPage.set(0);
        this.loadCatalogBooks();
      });

    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((searchTerm: string) => {
        this.filters.update((current: BookFilters) => ({
          ...current,
          searchTerm,
        }));

        this.currentPage.set(0);
        this.loadCatalogBooks();
      });
  }

  protected readonly filters = signal<BookFilters>({
    searchTerm: '',
    availability: 'ALL',
    didactic: 'ALL',
    minPages: null,
    maxPages: null,
    sortBy: 'AZ',
    readingLevelId: null,
    genreName: null,
  });

  protected readonly catalogSidebarHidden$ = combineLatest([
    this.userSettingsService.catalogFeaturedListsEnabled$,
    this.userSettingsService.catalogMostReadEnabled$,
    this.userSettingsService.catalogNewestEnabled$,
  ]).pipe(
    map(([featuredEnabled, mostReadEnabled, newestEnabled]) =>
      !featuredEnabled && !mostReadEnabled && !newestEnabled
    )
  );
  
  ngOnInit(): void {
    this.ensureCorrectSchool();

    const parsedFilters: BookFilters | null = this.getSavedFilters();

    if (parsedFilters) {
      this.filters.set(parsedFilters);
      this.searchInput.set(parsedFilters.searchTerm);
    }

    this.loadFilterBounds();
    this.loadCatalogBooks();
    this.loadReadingLevels();
    this.loadGenres();
    this.loadCampuses();
    this.loadSidebarBooks();
    this.loadFeaturedReadingLists();
    this.userSettingsService.loadCatalogSidebarSettings();
    this.loadFavoritesState();
  }

  private loadAccessibleSchools(): void {
    this.schoolsLoading.set(true);

    this.schoolService.getAccessibleSchools().subscribe({
      next: (schools) => {
        this.accessibleSchools.set(schools);
        this.schoolsLoading.set(false);
      },
      error: () => {
        this.accessibleSchools.set([]);
        this.schoolsLoading.set(false);
      },
    });
  }

  protected toggleSchoolDropdown(): void {
    this.isSchoolDropdownOpen.update((open) => !open);
  }

  protected selectSchoolFromDropdown(schoolId: number, schoolName: string): void {
    this.isSchoolDropdownOpen.set(false);

    this.onSchoolChange(schoolId, schoolName);
  }

  protected getSelectedSchoolTitle(): string {
    const selectedSchoolId = this.selectedSchoolId();

    if (selectedSchoolId === null) {
      return 'Kies een school';
    }

    const selectedIdAsNumber = Number(selectedSchoolId);

    const selected = this.accessibleSchools().find(
      (school) => school.id === selectedIdAsNumber,
    );

    return selected?.name || this.currentSchoolName() || 'Kies een school';
  }

  @HostListener('document:click', ['$event'])
  protected closeSchoolDropdown(event: MouseEvent): void {
    const target = event.target as HTMLElement;

    if (!target.closest('.school-dropdown')) {
      this.isSchoolDropdownOpen.set(false);
    }
  }

  protected onSchoolChange(schoolId: number | null, schoolName?: string): void {
    this.selectedSchoolService.selectSchool(
      schoolId,
      schoolName ?? '',
    );

    this.currentPage.set(0);
    this.loadCatalogBooks();
    this.loadSidebarBooks();
    this.loadFeaturedReadingLists();
  }

  private loadFeaturedReadingLists(): void {
    this.isLoadingFeaturedReadingLists = true;

    this.readingListService.getCatalogFeaturedLists(this.selectedSchoolId() ?? undefined).subscribe({
      next: (lists) => {
        this.featuredReadingLists = lists;
        this.isLoadingFeaturedReadingLists = false;
      },
      error: (error) => {
        console.error('Fout bij laden van uitgelichte leeslijsten', error);
        this.featuredReadingLists = [];
        this.isLoadingFeaturedReadingLists = false;
      },
    });
  }

  private loadCampuses(): void {
    this.campusService.getForCurrentUserSchool().subscribe({
      next: (campuses) => {
        this.hasMultipleCampuses.set(campuses.length > 1);
      },
      error: () => {
        this.hasMultipleCampuses.set(false);
      },
    });
  }

  private ensureCorrectSchool(): void {
    const user = this.authService.currentUser();

    if (this.canViewOtherSchools()) {
      return;
    }

    if (!user?.schoolId) {
      return;
    }

    if (Number(this.selectedSchoolId()) !== Number(user.schoolId)) {
      this.selectedSchoolService.selectSchool(user.schoolId, user.schoolName ?? '');
    }
  }

  private loadCatalogBooks(): void {
    this.catalogLoading.set(true);

    const filters: BookFilters = this.getNormalizedFilters(this.filters());

    this.bookService
      .getFilteredBooks(
        filters.searchTerm,
        filters.availability,
        filters.minPages,
        filters.maxPages,
        filters.readingLevelId,
        filters.genreName,
        filters.didactic,
        this.currentPage(),
        this.pageSize(),
        filters.sortBy,
        this.selectedSchoolId() ?? undefined,
      )
      .subscribe({
        next: (response: PaginatedResponse<Book>) => {
          this.books.set(response.data);
          this.pagingMeta.set(response.meta);
          this.catalogLoading.set(false);
        },
        error: () => {
          this.catalogLoading.set(false);
          this.toastService.show(
            'De catalogus kon niet worden geladen.',
            'error',
          );
        },
      });
  }



  private loadFilterBounds(): void {
    this.bookService.getMaxPages().subscribe({
      next: (max: number) => {
        this.absoluteMaxPages.set(max);

        if (this.filters().maxPages === null) {
          this.filters.update((current: BookFilters) => ({
            ...current,
            maxPages: max,
          }));
        }
      },
      error: () => {
        this.toastService.show(
          'Maximum aantal pagina’s kon niet worden geladen.',
          'error',
        );
      },
    });

    this.bookService.getMinPages().subscribe({
      next: (min: number) => {
        this.absoluteMinPages.set(min);

        if (this.filters().minPages === null) {
          this.filters.update((current: BookFilters) => ({
            ...current,
            minPages: min,
          }));
        }
      },
      error: () => {
        this.toastService.show(
          'Minimum aantal pagina’s kon niet worden geladen.',
          'error',
        );
      },
    });
  }

  private loadReadingLevels(): void {
    this.readingLevelsService.getAll().subscribe({
      next: (levels: ReadingLevel[]) => {
        this.readingLevels.set(levels);
      },
      error: () => {
        this.toastService.show(
          'De leesniveaus konden niet worden geladen.',
          'error',
        );
      },
    });
  }

  private loadGenres(): void {
    this.genreService.getGenres().subscribe({
      next: (genres: Genre[]) => {
        this.genres.set(genres);
      },
      error: () => {
        this.toastService.show(
          'De genres konden niet worden geladen.',
          'error',
        );
      },
    });
  }

  private loadSidebarBooks(): void {
    this.mostReadLoading.set(true);
    this.newestLoading.set(true);

    this.bookService.getMostReadBooks(3, this.selectedSchoolId() ?? undefined).subscribe({
      next: (books) => {
        this.mostReadBooks.set(books);
        this.mostReadLoading.set(false);
      },
      error: () => {
        this.mostReadBooks.set([]);
        this.mostReadLoading.set(false);
      },
    });

    this.bookService.getNewestBooks(3, this.selectedSchoolId() ?? undefined).subscribe({
      next: (books) => {
        this.newestBooks.set(books);
        this.newestLoading.set(false);
      },
      error: () => {
        this.newestBooks.set([]);
        this.newestLoading.set(false);
      },
    });
  }

  public updateSearchTerm(searchTerm: string): void {
    this.searchInput.set(searchTerm);
    this.searchSubject.next(searchTerm);
  }

  public updateAvailability(availability: BookFilters['availability']): void {
    this.filters.update((current: BookFilters) => ({
      ...current,
      availability,
    }));
    this.currentPage.set(0);
    this.loadCatalogBooks();
  }

  public updateDidactic(didactic: BookFilters['didactic']): void {
    this.filters.update((current: BookFilters) => ({
      ...current,
      didactic,
    }));
    this.currentPage.set(0);
    this.loadCatalogBooks();
  }

  public updateSortBy(sortBy: BookFilters['sortBy']): void {
    this.filters.update((current: BookFilters) => ({
      ...current,
      sortBy,
    }));
    this.currentPage.set(0);
    this.loadCatalogBooks();
  }

  public updateMaxPages(event: Event): void {
    const input = event.target as HTMLInputElement;
    let newMax = Number(input.value);

    const currentMin = this.filters().minPages ?? this.absoluteMinPages();

    if (newMax < currentMin) {
      newMax = currentMin;
      input.value = String(newMax);
    }

    this.filters.update((current: BookFilters) => ({
      ...current,
      maxPages: newMax,
    }));
    this.pageRangeSubject.next();
  }

  public updateMinPages(event: Event): void {
    const input = event.target as HTMLInputElement;
    let newMin = Number(input.value);

    const currentMax = this.filters().maxPages ?? this.absoluteMaxPages();

    if (newMin > currentMax) {
      newMin = currentMax;
      input.value = String(newMin);
    }

    this.filters.update((current: BookFilters) => ({
      ...current,
      minPages: newMin,
    }));
    this.pageRangeSubject.next();
  }

  public updateReadingLevel(readingLevelId: number | null): void {
    this.filters.update((current: BookFilters) => ({
      ...current,
      readingLevelId,
    }));

    this.currentPage.set(0);
    this.loadCatalogBooks();
  }

  public updateGenre(genreName: string | null): void {
    this.filters.update((current: BookFilters) => ({
      ...current,
      genreName,
    }));

    this.currentPage.set(0);
    this.loadCatalogBooks();
  }

  resetFilters(): void {
    this.filters.set({
      searchTerm: '',
      availability: 'ALL',
      didactic: 'ALL',
      minPages: this.absoluteMinPages(),
      maxPages: this.absoluteMaxPages(),
      sortBy: 'AZ',
      readingLevelId: null,
      genreName: null,
    });
    this.searchInput.set('');
    this.currentPage.set(0);
    this.loadCatalogBooks();
  }

  public toggleFavorite(event: Event, book: Book) {
    event.stopPropagation();
    if (!book.id) return;

    if (book.favorite) {
      const listId = this.favoritesListId();
      if (listId === null) return;

      this.readingListService.removeBookFromList(listId, book.id).subscribe({
        next: () => {
          this.books.update(prev =>
            prev.map(b => (b.id === book.id ? { ...b, favorite: false } : b))
          );
        },
        error: (err) => {
          console.error('Fout bij verwijderen uit favorieten', err);
          this.toastService.show(
            'Er is een fout opgetreden bij het verwijderen uit favorieten.',
            'error',
          );
        },
      });
    } else {
      this.readingListService.addBookToFavorites(book.id).subscribe({
        next: () => {
          this.books.update(prev =>
            prev.map(b => (b.id === book.id ? { ...b, favorite: true } : b))
          );
        },
        error: (err) => {
          if (err.error === 'Boek zit al in deze leeslijst') {
            this.books.update(prev =>
              prev.map(b =>
                b.id === book.id ? { ...b, favorite: true } : b
              )
            );
          } else {
            console.error('Fout bij toevoegen aan favorieten', err);
            this.toastService.show(
              'Er is een fout opgetreden bij het toevoegen aan favorieten.',
              'error',
            );
          }
        },
      });
    }
  }

  private loadFavoritesState(): void {
    this.readingListService.getFavoritesList().subscribe({
      next: (list) => {
        this.favoritesListId.set(list.id);
      },
      error: () => {
        console.error('Kon favorietenlijst niet laden');
      },
    });
  }

  private getSavedFilters(): BookFilters | null {
    const savedFilters: string | null = localStorage.getItem('bookFilters');

    if (!savedFilters) {
      return null;
    }

    try {
      const parsed = JSON.parse(savedFilters) as Partial<BookFilters>;
      return this.getNormalizedFilters(parsed);
    } catch {
      localStorage.removeItem('bookFilters');
      return null;
    }
  }

  private getNormalizedFilters(rawFilters: Partial<BookFilters>): BookFilters {
    return {
      searchTerm:
        typeof rawFilters.searchTerm === 'string' ? rawFilters.searchTerm : '',
      availability: this.normalizeAvailability(rawFilters.availability),
      didactic: this.canFilterDidacticBooks()
        ? this.normalizeDidactic(rawFilters.didactic)
        : 'ALL',
      minPages: this.normalizePageFilterValue(rawFilters.minPages),
      maxPages: this.normalizePageFilterValue(rawFilters.maxPages),
      sortBy: this.normalizeSortBy(rawFilters.sortBy),
      readingLevelId: this.normalizeReadingLevelId(rawFilters.readingLevelId),
      genreName: this.normalizeGenreName(rawFilters.genreName),
    };
  }

  private normalizeAvailability(value: unknown): BookFilters['availability'] {
    const normalized =
      typeof value === 'string' ? value.trim().toUpperCase() : '';

    if (normalized === 'AVAILABLE' || normalized === 'BESCHIKBAAR') {
      return 'AVAILABLE';
    }

    return 'ALL';
  }

  private normalizeDidactic(value: unknown): BookFilters['didactic'] {
    const normalized =
      typeof value === 'string' ? value.trim().toUpperCase() : '';

    if (normalized === 'DIDACTIC') {
      return 'DIDACTIC';
    }

    return 'ALL';
  }

  private normalizeSortBy(value: unknown): BookFilters['sortBy'] {
    const normalized =
      typeof value === 'string' ? value.trim().toUpperCase() : '';

    switch (normalized) {
      case 'ZA':
      case 'NEWEST':
      case 'RATING':
      case 'AUTHOR_AZ':
      case 'AUTHOR_ZA':
        return normalized;
      case 'AZ':
      default:
        return 'AZ';
    }
  }

  private normalizePageFilterValue(value: unknown): number | null {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }

    if (typeof value === 'string' && value.trim() !== '') {
      const parsed = Number(value);
      if (Number.isFinite(parsed)) {
        return parsed;
      }
    }

    return null;
  }

  private normalizeReadingLevelId(value: unknown): number | null {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }

    if (typeof value === 'string' && value.trim() !== '') {
      const parsed = Number(value);
      if (Number.isFinite(parsed)) {
        return parsed;
      }
    }

    return null;
  }

  private normalizeGenreName(value: unknown): string | null {
    if (typeof value !== 'string') {
      return null;
    }

    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : null;
  }

  public toBookDetailPage(book: Book) {
    this.router.navigate(['/book', book.id]);
  }

  protected onImgError(bookId: number | undefined): void {
    if (bookId !== undefined) {
      this.erroredImages.add(bookId);
    }
  }

  public onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadCatalogBooks();
  }
  
}
