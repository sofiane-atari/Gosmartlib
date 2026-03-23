import { Component, OnInit, signal, inject, computed, effect } from '@angular/core';
import { Book } from '../../model/book.model';
import { BookService } from '../../services/BookService/book.service';
import { ReadingListService } from '../../services/reading-list.service';
import { ToastService } from '../../services/Toast/toast.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { BookFilters } from '../../model/bookFilter.model';
import { DestroyRef } from '@angular/core';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  LucideAngularModule,
  Heart,
  X,
  Star,
  BookPlus,
  Search,
  SlidersHorizontal,
} from 'lucide-angular';
import { BookListSidebarComponent } from '../book-list-sidebar/book-list-sidebar.component';
import { PagingComponent } from '../paging/paging.component';
import { PaginatedResponse } from '../../model/paginated-response.model.ts';

@Component({
  selector: 'app-books',
  standalone: true,
  imports: [ FormsModule, CommonModule, LucideAngularModule, BookListSidebarComponent, PagingComponent],
  templateUrl: './books.component.html',
  styleUrl: './books.component.css',
})
export class BooksComponent implements OnInit {
  readonly Book = BookPlus;
  readonly Heart = Heart;
  readonly Search = Search;
  readonly SlidersHorizontal = SlidersHorizontal;
  readonly X = X;
  readonly Star = Star;

  private readonly bookService = inject(BookService);
  private readonly router = inject(Router);
  private readonly readingListService = inject(ReadingListService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef)

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

  protected readonly isFilterOpen = signal(false);
  protected readonly currentPage = signal(0);
  private readonly pageSize = signal(10);

  
  protected readonly availableCount = computed(() =>
    this.books().filter((book: Book) => (book.availableCopies ?? 0) > 0).length,
  );

  protected readonly borrowedCount = computed(() =>
    this.books().filter((book: Book) => (book.totalCopies ?? 0) > 0 && (book.availableCopies ?? 0) === 0).length,
  );
  

  constructor() {
    effect(() => {
      localStorage.setItem('bookFilters', JSON.stringify(this.filters()));
      localStorage.setItem('isFilterOpen', JSON.stringify(this.isFilterOpen()));
    });

    this.pageRangeSubject
    .pipe(
      debounceTime(400),
      takeUntilDestroyed(this.destroyRef),
    )
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
    minPages: null,
    maxPages: null,
    sortBy: 'AZ',
  });

  ngOnInit(): void {
    const parsedFilters: BookFilters | null = this.getSavedFilters();
    const savedPanelState = localStorage.getItem('isFilterOpen')

    if (parsedFilters) {
      this.filters.set(parsedFilters);
      this.searchInput.set(parsedFilters.searchTerm)
    }

    if (savedPanelState === 'true') {
      this.isFilterOpen.set(true);
    } else {
      this.isFilterOpen.set(false);
    }

    this.loadFilterBounds();
    this.loadCatalogBooks();
    this.loadSidebarBooks();
  }

  private loadCatalogBooks(): void {
    this.catalogLoading.set(true);

    const filters: BookFilters = this.filters();

    this.bookService.getFilteredBooks(
      filters.searchTerm,
      filters.availability,
      filters.minPages,
      filters.maxPages,
      this.currentPage(),
      this.pageSize(),
      filters.sortBy,
    ).subscribe({
      next: (response: PaginatedResponse<Book>) => {
        this.books.set(response.data);
        this.pagingMeta.set(response.meta);
        this.catalogLoading.set(false);
      },
      error: () => {
        this.catalogLoading.set(false);
        this.toastService.show('De catalogus kon niet worden geladen.', 'error');
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
        this.toastService.show('Maximum aantal pagina’s kon niet worden geladen.', 'error');
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
        this.toastService.show('Minimum aantal pagina’s kon niet worden geladen.', 'error');
      },
    });
  }

  private loadSidebarBooks(): void {
    this.bookService.getMostReadBooks(3).subscribe({
      next: (books) => {
        this.mostReadBooks.set(books);
        this.mostReadLoading.set(false);
      },
      error: () => {
        this.mostReadLoading.set(false);
      },
    });

    
    this.bookService.getNewestBooks(3).subscribe({
      next: (books) => {
        this.newestBooks.set(books);
        this.newestLoading.set(false);
      },
      error: () => {
        this.newestLoading.set(false);
      },
    });
  }

  public toggleFilters() {
    this.isFilterOpen.update((current: boolean) => !current);
  }

  public updateSearchTerm(searchTerm: string): void {
    this.searchInput.set(searchTerm)
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

  resetFilters(): void {
    this.filters.set({
      searchTerm: '',
      availability: 'ALL',
      minPages: this.absoluteMinPages(),
      maxPages: this.absoluteMaxPages(),
      sortBy: 'AZ',
    });
    this.searchInput.set('')
    this.currentPage.set(0);
    this.loadCatalogBooks();
  }

  public toggleFavorite(event: Event, book: Book) {
    event.stopPropagation();

    if (!book.id) {
      this.toastService.show('Het boek kon niet aan favorieten worden toegevoegd.', 'error');
      return;
    }
    this.readingListService.addBookToFavorites(book.id).subscribe({
      next: () => {
        this.toastService.show(`"${book.title}" is toegevoegd aan je Favorieten!`, 'success');
      },
      error: (err) => {
        if (err.error === 'Boek zit al in deze leeslijst') {
          this.toastService.show(`"${book.title}" staat al in je favorieten.`, 'error');
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

  private getSavedFilters(): BookFilters | null {
    const savedFilters: string | null = localStorage.getItem('bookFilters');

    if (!savedFilters) {
      return null;
    }

    try {
      return JSON.parse(savedFilters) as BookFilters;
    } catch {
      localStorage.removeItem('bookFilters');
      return null;
    }
  }

  public toBookDetailPage(book: Book) {
    this.router.navigate(['/book', book.id]);
  }

  public onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadCatalogBooks();
  }
}