import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Book } from '../../model/book.model';
import { BookService } from '../../services/BookService/book.service';
import { LoansService } from '../../services/loans.service';
import { UserService } from '../../services/user.service';
import { AuthUserDto } from '../../model/auth.model';
import { PaginatedResponse } from '../../model/paginated-response.model.ts';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { toLocalDate } from '../../shared/types/local-date.type';
import { ToastService } from '../../services/Toast/toast.service';

@Component({
  selector: 'app-create-loan',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './create-loan.component.html',
  styleUrl: './create-loan.component.css',
})
export class CreateLoanComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly userService = inject(UserService);
  private readonly bookService = inject(BookService);
  private readonly loansService = inject(LoansService);
  private readonly toastService = inject(ToastService);
  private readonly catalogSearchSubject = new Subject<string>();
  private readonly isbnSearchSubject = new Subject<string>();
  private readonly scannerSuffixKey = 'Enter';
  private readonly scannerConfiguredPrefixes = ['SCN:', 'ISBN:'];
  private readonly scannerMinLength = 10;
  private readonly scannerMaxDurationMs = 1200;
  private readonly scannerMaxInterKeyDelayMs = 120;

  private isbnScanStartTimestamp = 0;
  private isbnScanLastTimestamp = 0;
  private isbnScanKeystrokeCount = 0;
  private suppressNextDebouncedIsbnSearch = false;
  private latestIsbnRequestSequence = 0;

  public readonly isLoadingUsers = signal(false);
  public readonly isLoadingCatalogBooks = signal(false);
  public readonly isLoadingIsbnBooks = signal(false);
  public readonly isSubmitting = signal(false);

  public readonly searchFirstname = signal('');
  public readonly searchLastname = signal('');
  // TODO: TEMPORARY - Remove this signal when searchId feature is no longer needed
  public readonly searchId = signal('');
  public readonly bookSearchQuery = signal('');
  public readonly bookSearchIsbn = signal('');
  public readonly users = signal<AuthUserDto[]>([]);
  public readonly catalogBooks = signal<Book[]>([]);
  public readonly isbnBooks = signal<Book[]>([]);
  public readonly selectedUser = signal<AuthUserDto | null>(null);
  public readonly selectedBooks = signal<Book[]>([]);
  public readonly minReturnDate = signal(this.buildMinReturnDate());

  public readonly isLoadingBooks = computed(() => {
    return this.isLoadingCatalogBooks() || this.isLoadingIsbnBooks();
  });

  public readonly hasBookSearchInput = computed(() => {
    return (
      this.bookSearchQuery().trim().length > 0 ||
      this.bookSearchIsbn().trim().length > 0
    );
  });

  protected loanForm = this.fb.group({
    returnDate: ['', Validators.required],
  });

  public constructor() {
    this.catalogSearchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((searchTerm: string) => {
        if (!this.selectedUser()) {
          return;
        }

        if (searchTerm.length === 0) {
          this.catalogBooks.set([]);
          return;
        }

        this.loadCatalogBooks(searchTerm);
      });

    this.isbnSearchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((isbn: string) => {
        if (this.suppressNextDebouncedIsbnSearch) {
          this.suppressNextDebouncedIsbnSearch = false;
          return;
        }

        if (!this.selectedUser()) {
          return;
        }

        if (isbn.length === 0) {
          this.isbnBooks.set([]);
          return;
        }

        this.fetchBookByIsbn(isbn, false);
      });
  }

  public readonly filteredBooks = computed(() => {
    const selectedBookIds = new Set(this.selectedBooks().map((book) => book.id));
    const mergedBooksByKey = new Map<string, Book>();

    [...this.catalogBooks(), ...this.isbnBooks()].forEach((book) => {
      const key = book.id !== undefined ? `id-${book.id}` : `isbn-${book.ISBN}`;
      if (!mergedBooksByKey.has(key)) {
        mergedBooksByKey.set(key, book);
      }
    });

    return Array.from(mergedBooksByKey.values()).filter((book) => {
      const hasId = book.id !== undefined;
      const notAlreadySelected = !selectedBookIds.has(book.id);
      const hasCopiesAvailable = (book.availableCopies ?? 0) > 0;

      return hasId && notAlreadySelected && hasCopiesAvailable;
    });
  });

  public onFirstnameSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchFirstname.set(target.value);
  }

  public onLastnameSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchLastname.set(target.value);
  }

  // TODO: TEMPORARY - Remove this method when searchId feature is no longer needed
  public onSearchIdChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchId.set(target.value);
  }

  public searchUsers(): void {
    const firstName = this.searchFirstname().trim();
    const lastName = this.searchLastname().trim();
    // TODO: TEMPORARY - Remove searchId parameter when feature is no longer needed
    const id = this.searchId().trim();
    this.loadUsers(firstName, lastName, id);
  }

  public selectUser(user: AuthUserDto): void {
    this.selectedUser.set(user);
    this.selectedBooks.set([]);
    this.triggerCatalogSearch();
    this.triggerIsbnSearch();
  }

  public clearSelectedUser(): void {
    this.selectedUser.set(null);
    this.selectedBooks.set([]);
    this.catalogBooks.set([]);
    this.isbnBooks.set([]);
    this.resetIsbnScannerTracking();
    this.bookSearchQuery.set('');
    this.bookSearchIsbn.set('');
  }

  public onBookSearchQueryChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.bookSearchQuery.set(target.value);
    this.triggerCatalogSearch();
  }

  public onBookIsbnSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.bookSearchIsbn.set(target.value);
    this.triggerIsbnSearch(this.normalizeIsbnInput(target.value));
  }

  public onBookIsbnKeydown(event: KeyboardEvent): void {
    if (event.key === this.scannerSuffixKey) {
      event.preventDefault();

      const rawInput = this.bookSearchIsbn().trim();
      if (rawInput.length === 0) {
        return;
      }

      const scannerPayload = this.extractScannerPayload(rawInput);
      const normalizedIsbn = this.normalizeIsbnInput(scannerPayload.payload);

      if (!this.isValidIsbnFormat(normalizedIsbn)) {
        this.showError('Het ingevoerde ISBN-formaat is ongeldig. Gebruik een geldig ISBN-10 of ISBN-13.');
        this.resetIsbnScannerTracking();
        return;
      }

      this.suppressNextDebouncedIsbnSearch = true;
      this.bookSearchIsbn.set(normalizedIsbn);

      const isLikelyScanner = scannerPayload.hasConfiguredPrefix || this.isLikelyScannerInput(normalizedIsbn);
      this.fetchBookByIsbn(normalizedIsbn, isLikelyScanner);
      this.resetIsbnScannerTracking();
      return;
    }

    if (event.key === 'Backspace' || event.key === 'Delete') {
      this.resetIsbnScannerTracking();
      return;
    }

    if (event.key.length !== 1 || event.ctrlKey || event.altKey || event.metaKey) {
      return;
    }

    const now = Date.now();
    const isFirstKeystroke = this.isbnScanKeystrokeCount === 0;
    const isNewSequence = now - this.isbnScanLastTimestamp > this.scannerMaxInterKeyDelayMs;

    if (isFirstKeystroke || isNewSequence) {
      this.isbnScanStartTimestamp = now;
      this.isbnScanKeystrokeCount = 1;
    } else {
      this.isbnScanKeystrokeCount += 1;
    }

    this.isbnScanLastTimestamp = now;
  }

  public addBook(book: Book): void {
    if (book.id === undefined || (book.availableCopies ?? 0) < 1) {
      return;
    }

    const isAlreadySelected = this.selectedBooks().some((selectedBook) => selectedBook.id === book.id);
    if (isAlreadySelected) {
      return;
    }

    this.selectedBooks.update((books) => [...books, book]);
  }

  public removeBook(bookId: number | undefined): void {
    if (bookId === undefined) {
      return;
    }

    this.selectedBooks.update((books) => books.filter((book) => book.id !== bookId));
  }

  public createLoan(): void {
    const user = this.selectedUser();
    const books = this.selectedBooks();
    const returnDate = this.loanForm.controls.returnDate.value;

    if (!user || books.length === 0 || !returnDate) {
      this.showError('Vul alle verplichte velden in voor je een lening aanmaakt.');
      return;
    }

    if (returnDate < this.minReturnDate()) {
      this.showError('De terugbrengdatum moet minimaal één dag in de toekomst liggen.');
      return;
    }

    const bookIds = books
      .map((book) => book.id)
      .filter((bookId): bookId is number => bookId !== undefined);

    if (bookIds.length !== books.length) {
      this.showError('Eén of meerdere geselecteerde boeken zijn ongeldig.');
      return;
    }

    this.isSubmitting.set(true);

    this.loansService
      .createLoan({
        id: 0,
        userId: user.id,
        bookIds: bookIds,
        returnDate: toLocalDate(new Date(returnDate)),
      })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.showSuccess('De lening is succesvol aangemaakt.');
          this.selectedBooks.set([]);
          this.loanForm.reset({ returnDate: '' });
          this.triggerCatalogSearch();
          this.triggerIsbnSearch();
        },
        error: () => {
          this.isSubmitting.set(false);
          this.showError('De lening kon niet worden aangemaakt. Probeer opnieuw.');
        },
      });
  }

  protected trackByBookId(index: number, book: Book): number {
    return book.id ?? index;
  }

  // TODO: TEMPORARY - Remove 'id' parameter when searchId feature is no longer needed
  private loadUsers(firstName: string = '', lastName: string = '', id: string = ''): void {
    this.isLoadingUsers.set(true);

    this.userService
      .getUsers({ WithfirstName: firstName, WithlastName: lastName , WithId: id})
      .subscribe({
        next: (users) => {
          this.users.set(users);
          this.isLoadingUsers.set(false);
        },
        error: (err: any) => {
          this.users.set([]);
          this.isLoadingUsers.set(false);
          this.showError('Gebruikers konden niet worden geladen.');
          console.log('Error loading users:', err);
          console.log('Error loading users with parameters:', { firstName, lastName, id });
        },
      });
  }

  private loadCatalogBooks(searchTerm: string = ''): void {
    if (!this.selectedUser()) {
      this.catalogBooks.set([]);
      return;
    }

    this.isLoadingCatalogBooks.set(true);

    this.bookService.getFilteredBooks(
      searchTerm.length > 0 ? searchTerm : null,
      'AVAILABLE',
      null,
      null,
      0,
      50,
      'AZ',
    ).subscribe({
      next: (response: PaginatedResponse<Book>) => {
        this.catalogBooks.set(response.data);
        this.isLoadingCatalogBooks.set(false);
      },
      error: () => {
        this.catalogBooks.set([]);
        this.isLoadingCatalogBooks.set(false);
        this.showError('Boeken konden niet worden geladen.');
      },
    });
  }

  private fetchBookByIsbn(isbn: string, autoSelect: boolean): void {
    if (!this.selectedUser()) {
      this.isbnBooks.set([]);
      return;
    }

    const requestSequence = ++this.latestIsbnRequestSequence;
    this.isLoadingIsbnBooks.set(true);

    this.bookService.getBookByISBN(isbn).subscribe({
      next: (book: Book) => {
        if (requestSequence !== this.latestIsbnRequestSequence) {
          return;
        }

        this.isbnBooks.set([book]);
        this.isLoadingIsbnBooks.set(false);

        if (autoSelect) {
          this.selectScannedBook(book);
        }
      },
      error: () => {
        if (requestSequence !== this.latestIsbnRequestSequence) {
          return;
        }

        this.isbnBooks.set([]);
        this.isLoadingIsbnBooks.set(false);
      },
    });
  }

  private selectScannedBook(book: Book): void {
    if (book.id === undefined) {
      return;
    }

    const hasCopiesAvailable = (book.availableCopies ?? 0) > 0;
    if (!hasCopiesAvailable) {
      this.showError('Het gescande boek is niet beschikbaar voor uitlening.');
      return;
    }

    const isAlreadySelected = this.selectedBooks().some((selectedBook) => selectedBook.id === book.id);
    if (isAlreadySelected) {
      this.showSuccess('Het gescande boek was al geselecteerd.');
      return;
    }

    this.addBook(book);
    this.showSuccess('Boek succesvol gescand en toegevoegd.');
  }

  private isLikelyScannerInput(isbn: string): boolean {
    const keyPressCount = this.isbnScanKeystrokeCount;
    const scanDuration = this.isbnScanLastTimestamp - this.isbnScanStartTimestamp;

    return (
      keyPressCount >= Math.min(this.scannerMinLength, isbn.length) &&
      scanDuration > 0 &&
      scanDuration <= this.scannerMaxDurationMs
    );
  }

  private extractScannerPayload(rawInput: string): { payload: string; hasConfiguredPrefix: boolean } {
    const trimmedInput = rawInput.trim();
    const upperInput = trimmedInput.toUpperCase();

    for (const prefix of this.scannerConfiguredPrefixes) {
      if (upperInput.startsWith(prefix)) {
        return {
          payload: trimmedInput.slice(prefix.length).trim(),
          hasConfiguredPrefix: true,
        };
      }
    }

    return {
      payload: trimmedInput,
      hasConfiguredPrefix: false,
    };
  }

  private normalizeIsbnInput(value: string): string {
    const scannerPayload = this.extractScannerPayload(value).payload;
    return scannerPayload.replace(/[\s-]/g, '').trim();
  }

  private isValidIsbnFormat(isbn: string): boolean {
    const isIsbn13 = /^\d{13}$/.test(isbn);
    const isIsbn10 = /^\d{9}[\dXx]$/.test(isbn);
    return isIsbn10 || isIsbn13;
  }

  private resetIsbnScannerTracking(): void {
    this.isbnScanStartTimestamp = 0;
    this.isbnScanLastTimestamp = 0;
    this.isbnScanKeystrokeCount = 0;
  }

  private triggerCatalogSearch(): void {
    this.catalogSearchSubject.next(this.bookSearchQuery().trim());
  }

  private triggerIsbnSearch(isbnOverride?: string): void {
    this.isbnSearchSubject.next((isbnOverride ?? this.bookSearchIsbn().trim()));
  }

  private showSuccess(message: string): void {
    this.toastService.show(message, 'success');
  }

  private showError(message: string): void {
    this.toastService.show(message, 'error');
  }

  private buildMinReturnDate(): string {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return toLocalDate(tomorrow);
  }
}