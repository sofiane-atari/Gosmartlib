import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { Book } from '../../model/book.model';
import { BookService } from '../../services/BookService/book.service';
import { ReadingListService } from '../../services/reading-list.service';
import { ToastService } from '../../services/Toast/toast.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  Heart,
  BookOpen,
  X,
  Library,
  Minus,
  Plus,
  CheckCircle,
  Edit2,
  Trash2,
  Calendar,
  Star,
  MessageCircle,
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

  // Signals for reactive state
  books = signal<Book[]>([]);
  filteredBooks = signal<Book[]>([]);
  mostReadBooks = signal<Book[]>([]);
  newestBooks = signal<Book[]>([]);
  mostReadLoading = signal<boolean>(true);
  newestLoading = signal<boolean>(true);

  absoluteMinYear: number = 0;
  absoluteMaxYear: number = 0;
  absoluteMaxPages: number = 0;

  isFilterOpen: boolean = false;
  availableCount: number = 0;
  borrowedCount: number = 0;

  filters = {
    searchTerm: '',
    availability: 'Alle Boeken',
    minYear: null as number | null,
    maxYear: null as number | null,
    maxPages: null as number | null,
    sortBy: 'az',
  };

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
    const savedFilters = localStorage.getItem('bookFilters');
    const parsedFilters = savedFilters ? JSON.parse(savedFilters) : null;

    if (parsedFilters) {
      this.filters = parsedFilters;
      this.isFilterOpen = true;
    }

    this.loadBooks(parsedFilters);
    this.loadSidebarBooks();
  }

  private loadBooks(parsedFilters: any): void {
    this.bookService.getMaxPages().subscribe((max) => {
      this.absoluteMaxPages = max;
      if (!parsedFilters || parsedFilters.maxPages === null) {
        this.filters.maxPages = max;
      }
    });

    this.bookService.getYearRange().subscribe((range) => {
      this.absoluteMinYear = range.min;
      this.absoluteMaxYear = range.max;

      if (!parsedFilters) {
        this.filters.minYear = range.min;
        this.filters.maxYear = range.max;
      } else {
        if (this.filters.minYear === null) this.filters.minYear = range.min;
        if (this.filters.maxYear === null) this.filters.maxYear = range.max;
      }
    });

    this.bookService.getBooks().subscribe((data) => {
      this.books.set(data);
      this.filteredBooks.set(data);
      // this.calculateStats();
      this.applyFilters();
    });
  }

  private loadSidebarBooks(): void {
    // Load most read books
    this.bookService.getMostReadBooks(3).subscribe({
      next: (books) => {
        this.mostReadBooks.set(books);
        this.mostReadLoading.set(false);
      },
      error: () => {
        this.mostReadLoading.set(false);
      },
    });

    // Load newest books
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
    this.isFilterOpen = !this.isFilterOpen;
  }

  // calculateStats() {
  //   const booksArray = this.books();
  //   this.availableCount = booksArray.filter(
  //     (b) => b.availability === 'AVAILABLE',
  //   ).length;
  //   this.borrowedCount = booksArray.filter(
  //     (b) => b.availability === 'BORROWED',
  //   ).length;
  // }

  applyFilters() {
    localStorage.setItem('bookFilters', JSON.stringify(this.filters));

    const booksArray = this.books();
    const filtered = booksArray.filter((book) => {
      const searchMatch =
        !this.filters.searchTerm ||
        book.title
          .toLowerCase()
          .includes(this.filters.searchTerm.toLowerCase()) ||
        book.author
          .toLowerCase()
          .includes(this.filters.searchTerm.toLowerCase());

      let availabilityMatch = true;
      // if (this.filters.availability === 'Beschikbaar') {
      //   availabilityMatch = book.availability === 'AVAILABLE';
      // } else if (this.filters.availability === 'Uitgeleend') {
      //   availabilityMatch = book.availability === 'BORROWED';
      // }

      const maxPagesMatch =
        !this.filters.maxPages || book.pages <= this.filters.maxPages;

      const minYearMatch =
        !this.filters.minYear || book.releaseYear >= this.filters.minYear;
      const maxYearMatch =
        !this.filters.maxYear || book.releaseYear <= this.filters.maxYear;

      return (
        searchMatch &&
        availabilityMatch &&
        minYearMatch &&
        maxYearMatch &&
        maxPagesMatch
      );
    });

    this.filteredBooks.set(filtered);
    this.sortBooks();
    this.currentPage.set(0);
  }

  resetFilters() {
    localStorage.removeItem('bookFilters');

    this.filters.searchTerm = '';
    this.filters.availability = 'Alle Boeken';
    this.filters.minYear = this.absoluteMinYear;
    this.filters.maxYear = this.absoluteMaxYear;
    this.filters.maxPages = this.absoluteMaxPages;
    this.filters.sortBy = 'az';

    this.applyFilters();
  }

  sortBooks() {
    const filteredArray = [...this.filteredBooks()];

    if (this.filters.sortBy === 'az') {
      filteredArray.sort((a, b) => a.title.localeCompare(b.title));
    } else if (this.filters.sortBy === 'za') {
      filteredArray.sort((a, b) => b.title.localeCompare(a.title));
    } else if (this.filters.sortBy === 'newest') {
      filteredArray.sort((a, b) => b.releaseYear - a.releaseYear);
    } else if (this.filters.sortBy === 'rating') {
      filteredArray.sort((a, b) => (b.reviewScore || 0) - (a.reviewScore || 0));
    }

    this.filteredBooks.set(filteredArray);
    this.ensureCurrentPageInRange();
  }

  toggleFavorite(event: Event, book: Book) {
    event.stopPropagation();
    if (book.id) {
      this.readingListService.addBookToFavorites(book.id).subscribe({
        next: () => {
          this.toastService.show(`"${book.title}" is toegevoegd aan je Favorieten!`, 'success');
        },
        error: (err: any) => {
          if (err.error === 'Boek zit al in deze leeslijst') {
            this.toastService.show(`"${book.title}" staat al in je Favorieten!`, 'error');
          } else {
            console.error('Fout bij toevoegen aan favorieten', err);
            this.toastService.show(
              'Er is een fout opgetreden bij het toevoegen aan favorieten.',
              'error'
            );
          }
        },
      });
    }
  }

  toBookDetailPage(book: Book) {
    this.router.navigate(['/book', book.id]);
  }

  protected onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  private ensureCurrentPageInRange(): void {
    const meta = this.pagingMeta();

    if (meta.totalPages === 0) {
      this.currentPage.set(0);
      return;
    }

    if (this.currentPage() > meta.totalPages - 1) {
      this.currentPage.set(meta.totalPages - 1);
    }
  }
}
