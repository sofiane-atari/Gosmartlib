import { Component, OnInit, signal, inject, computed, effect } from '@angular/core';
import { Book } from '../../model/book.model';
import { BookService } from '../../services/BookService/book.service';
import { ReadingListService } from '../../services/reading-list.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { BookFilters } from '../../model/bookFilter.model';
import { ToastService } from '../../services/Toast/toast.service';
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

@Component({
  selector: 'app-books',
  standalone: true,
  imports: [
    FormsModule,
    CommonModule,
    LucideAngularModule,
    BookListSidebarComponent,
    PagingComponent,
  ],
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

  books = signal<Book[]>([]);
  mostReadBooks = signal<Book[]>([]);
  newestBooks = signal<Book[]>([]);
  mostReadLoading = signal<boolean>(true);
  newestLoading = signal<boolean>(true);
  absoluteMaxPages = signal<number>(0);

  constructor() {
    effect(() => {
      localStorage.setItem('bookFilters', JSON.stringify(this.filters()));
      localStorage.setItem('isFilterOpen', JSON.stringify(this.isFilterOpen()));
    });


  }

  protected readonly isFilterOpen = signal(false);
  
  protected readonly availableCount = computed(() =>
    this.books().filter((book: Book) => book.availability === 'AVAILABLE').length,
  );

  protected readonly borrowedCount = computed(() =>
    this.books().filter((book: Book) => book.availability === 'BORROWED').length,
  );

  protected readonly filters = signal<BookFilters>({
    searchTerm: '',
    availability: 'Alle Boeken',
    maxPages: null,
    sortBy: 'az',
  });

  protected readonly filteredBooks = computed(() => {
    const booksArray = this.books();
    const currentFilters = this.filters();

    const filtered = booksArray.filter((book) => {
      const searchMatch =
        !currentFilters.searchTerm ||
        book.title.toLowerCase().includes(currentFilters.searchTerm.toLowerCase()) ||
        book.author.toLowerCase().includes(currentFilters.searchTerm.toLowerCase());

      let availabilityMatch = true;
      if (currentFilters.availability === 'Beschikbaar') {
        availabilityMatch = book.availability === 'AVAILABLE';
      }

      const maxPagesMatch =
        currentFilters.maxPages === null || book.pages <= currentFilters.maxPages;

      return searchMatch && availabilityMatch && maxPagesMatch;
    });

    const sortBy = currentFilters.sortBy;
    if (sortBy === 'az') {
      filtered.sort((a, b) => a.title.localeCompare(b.title));
    } else if (sortBy === 'za') {
      filtered.sort((a, b) => b.title.localeCompare(a.title));
    } else if (sortBy === 'newest') {
      filtered.sort((a, b) => b.releaseYear - a.releaseYear);
    } else if (sortBy === 'rating') {
      filtered.sort((a, b) => (b.reviewScore || 0) - (a.reviewScore || 0));
    }

    return filtered;
  });

  private readonly pageSize = signal<number>(10);
  protected readonly currentPage = signal<number>(0);

  protected readonly pagingMeta = computed(() => {
    const totalItems = this.filteredBooks().length;
    const pageSize = this.pageSize();
    const totalPages = totalItems === 0 ? 0 : Math.ceil(totalItems / pageSize);

    return {
      currentPage: this.currentPage(),
      pageSize,
      totalItems,
      totalPages,
    };
  });

  protected readonly pagedBooks = computed(() => {
    const start = this.currentPage() * this.pageSize();
    return this.filteredBooks().slice(start, start + this.pageSize());
  });

  ngOnInit(): void {
    const parsedFilters: BookFilters | null = this.getSavedFilters();
    const savedPanelState = localStorage.getItem('isFilterOpen')

    if (parsedFilters) {
      this.filters.set(parsedFilters);
    }

    if (savedPanelState === 'true') {
      this.isFilterOpen.set(true);
    } else {
      this.isFilterOpen.set(false);
    }

    this.loadBooks(parsedFilters);
    this.loadSidebarBooks();
  }

  private loadBooks(parsedFilters: BookFilters | null): void {
    this.bookService.getMaxPages().subscribe((max) => {
      this.absoluteMaxPages.set(max);
      if (!parsedFilters || parsedFilters.maxPages === null) {
        this.filters.update((current: BookFilters) => ({
          ...current,
          maxPages: max,
        }));
      }
    });

    this.bookService.getBooks().subscribe((data) => {
      this.books.set(data);
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

  toggleFilters() {
    this.isFilterOpen.update((current: boolean) => !current);
  }

  public updateSearchTerm(searchTerm: string): void {
    this.filters.update((current: BookFilters) => ({
      ...current,
      searchTerm,
    }));
    this.currentPage.set(0);
  }

  public updateAvailability(availability: BookFilters['availability']): void {
    this.filters.update((current: BookFilters) => ({
      ...current,
      availability,
    }));
    this.currentPage.set(0);
  }

  public updateSortBy(sortBy: BookFilters['sortBy']): void {
    this.filters.update((current: BookFilters) => ({
      ...current,
      sortBy,
    }));
    this.currentPage.set(0);
  }

  public updateMaxPages(maxPages: number): void {
    this.filters.update((current: BookFilters) => ({
      ...current,
      maxPages: Number(maxPages),
    }));
    this.currentPage.set(0);
  }

  resetFilters(): void {
    localStorage.removeItem('bookFilters');

    this.filters.set({
      searchTerm: '',
      availability: 'Alle Boeken',
      maxPages: this.absoluteMaxPages(),
      sortBy: 'az',
    });
    this.currentPage.set(0);
  }

  toggleFavorite(event: Event, book: Book) {
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

  toBookDetailPage(book: Book) {
    this.router.navigate(['/book', book.id]);
  }

  public onPageChange(page: number): void {
    this.currentPage.set(page);
  }
}