// didactic-material-book-selection.service.ts
import {
  computed,
  DestroyRef,
  inject,
  Injectable,
  signal,
} from '@angular/core';
import { toObservable, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  catchError,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  finalize,
  forkJoin,
  map,
  of,
  startWith,
  switchMap,
} from 'rxjs';
import { BookService } from '../bookService/book.service';
import { ToastService } from '../toast/toast.service';
import { BookCandidate } from '../../model/book/book-candidate.model';
import { DidacticMaterialBookSummary } from '../../model/didactic/didactic-material.model';
import { Book } from '../../model/book/book.model';

@Injectable()
export class DidacticMaterialBookSelectionService {
  private readonly bookService = inject(BookService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  public readonly searchQuery = signal('');
  public readonly searchIsbn = signal('');
  public readonly isLoadingBooks = signal(false);
  public readonly filteredBooks = signal<BookCandidate[]>([]);
  public readonly selectedLinkedBooks = signal<DidacticMaterialBookSummary[]>(
    [],
  );

  public readonly hasBookSearchInput = computed(
    () =>
      this.searchQuery().trim().length > 0 ||
      this.searchIsbn().trim().length > 0,
  );

  public readonly selectedLinkedBooksCount = computed(
    () => this.selectedLinkedBooks().length,
  );

  public readonly selectedLinkedBookIds = computed(() =>
    this.selectedLinkedBooks().map((book) => book.id),
  );

  public constructor() {
    this.setupBookSearch();
  }

  public setSearchQuery(value: string): void {
    this.searchQuery.set(value);
  }

  public setSearchIsbn(value: string): void {
    this.searchIsbn.set(value);
  }

  public hydrateSelectedBooks(books: DidacticMaterialBookSummary[]): void {
    this.selectedLinkedBooks.set([...books]);
  }

  public addSelectedBook(book: BookCandidate): void {
    const bookId = book.id;

    if (bookId === undefined) {
      this.toastService.show('Dit boek heeft geen geldig id.', 'error');
      return;
    }

    const alreadySelected = this.selectedLinkedBooks().some(
      (selectedBook) => selectedBook.id === bookId,
    );

    if (alreadySelected) {
      this.toastService.show('Dit boek is al gekoppeld.', 'error');
      return;
    }

    this.selectedLinkedBooks.update((currentBooks) => [
      ...currentBooks,
      {
        id: bookId,
        title: book.title,
        author: book.author,
      },
    ]);
  }

  public removeSelectedBook(bookId: number): void {
    this.selectedLinkedBooks.update((currentBooks) =>
      currentBooks.filter((book) => book.id !== bookId),
    );
  }

  public resetSearch(): void {
    this.searchQuery.set('');
    this.searchIsbn.set('');
    this.filteredBooks.set([]);
    this.isLoadingBooks.set(false);
  }

  private setupBookSearch(): void {
    const searchQuery$ = toObservable(this.searchQuery).pipe(
      startWith(this.searchQuery()),
      debounceTime(400),
      distinctUntilChanged(),
    );

    const searchIsbn$ = toObservable(this.searchIsbn).pipe(
      startWith(this.searchIsbn()),
      debounceTime(400),
      distinctUntilChanged(),
    );

    combineLatest([searchQuery$, searchIsbn$])
      .pipe(
        switchMap(([searchQuery, searchIsbn]) => {
          const normalizedQuery = searchQuery.trim();
          const normalizedIsbn = searchIsbn.trim();

          if (!normalizedQuery && !normalizedIsbn) {
            this.filteredBooks.set([]);
            this.isLoadingBooks.set(false);
            return of([] as BookCandidate[]);
          }

          this.isLoadingBooks.set(true);

          if (normalizedIsbn) {
            return this.bookService.getBookByISBN(normalizedIsbn).pipe(
              map((book) => this.mapBookToCandidates(book, normalizedIsbn)),
              catchError(() => of([] as BookCandidate[])),
              finalize(() => this.isLoadingBooks.set(false)),
            );
          }

          return forkJoin([
            this.bookService
              .searchBooksInDb(normalizedQuery, undefined, undefined)
              .pipe(catchError(() => of([] as BookCandidate[]))),
            this.bookService
              .searchBooksInDb(undefined, normalizedQuery, undefined)
              .pipe(catchError(() => of([] as BookCandidate[]))),
          ]).pipe(
            map(([titleMatches, authorMatches]) =>
              this.mergeBookCandidates(titleMatches, authorMatches),
            ),
            finalize(() => this.isLoadingBooks.set(false)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((books) => {
        this.filteredBooks.set(books);
      });
  }

  private mapBookToCandidates(
    book: Book,
    fallbackIsbn: string,
  ): BookCandidate[] {
    if (book.id === undefined) {
      return [];
    }

    return [
      {
        id: book.id,
        title: book.title,
        author: book.author,
        isbn13: book.isbn ?? fallbackIsbn,
        releaseYear: book.releaseYear ?? null,
        imageUrl: book.imageUrl || undefined,
      },
    ];
  }

  private mergeBookCandidates(
    ...candidateGroups: BookCandidate[][]
  ): BookCandidate[] {
    const uniqueCandidates = new Map<number, BookCandidate>();

    for (const candidateGroup of candidateGroups) {
      for (const candidate of candidateGroup) {
        if (candidate.id === undefined) {
          continue;
        }

        if (!uniqueCandidates.has(candidate.id)) {
          uniqueCandidates.set(candidate.id, candidate);
        }
      }
    }

    return Array.from(uniqueCandidates.values());
  }
}
