import { CommonModule } from '@angular/common';
import {
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
  effect,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule, ArrowLeft } from 'lucide-angular';
import { Book, BookCopyLocation } from '../../../model/book/book.model';
import { BookService } from '../../../services/bookService/book.service';
import { LoansService } from '../../../services/loan/loans.service';
import { UserService } from '../../../services/user/user.service';
import { AuthService } from '../../../services/authorization/auth.service';
import { AuthUserDto, SchoolSummaryDto } from '../../../model/auth.model';
import { School } from '../../../model/school.model';
import { SchoolService } from '../../../services/school/school.service';
import { PaginatedResponse } from '../../../model/shared/paging/PaginatedResponse.model';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { toLocalDate } from '../../../shared/types/local-date.type';
import { ToastService } from '../../../services/toast/toast.service';
import { UserSelectionPanelComponent } from './user-selection-panel/user-selection-panel.component';
import { BookSelectionPanelComponent } from './book-selection-panel/book-selection-panel.component';
import { LoanSummaryPanelComponent } from './loan-summary-panel/loan-summary-panel.component';
import { ScanIsbnComponent } from './scan-isbn/scan-isbn.component';
import { ConfirmationModalComponent } from '../../shared/confirmation-modal/confirmation-modal.component';
import {
  CREATE_LOAN_WIZARD_STEPS,
  CreateLoanWizardStep,
} from '../../../model/loan/loan-wizard.model';
import { StepWizardComponent } from '../../shared/step-wizard/step-wizard.component';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';

@Component({
  selector: 'app-create-loan',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LucideAngularModule,
    UserSelectionPanelComponent,
    BookSelectionPanelComponent,
    LoanSummaryPanelComponent,
    ScanIsbnComponent,
    ConfirmationModalComponent,
    StepWizardComponent,
    ButtonHoverHelpComponent,
  ],
  templateUrl: './create-loan.component.html',
  styleUrl: './create-loan.component.css',
})
export class CreateLoanComponent {
  protected readonly ArrowLeft = ArrowLeft;

  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly userService = inject(UserService);
  private readonly schoolService = inject(SchoolService);
  private readonly bookService = inject(BookService);
  private readonly loansService = inject(LoansService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly catalogSearchSubject = new Subject<string>();
  private readonly isbnSearchSubject = new Subject<string>();
  private suppressNextDebouncedIsbnSearch = false;
  private latestIsbnRequestSequence = 0;

  private readonly authService = inject(AuthService);

  public readonly isLoadingUsers = signal(false);
  public readonly isLoadingSchools = signal(false);
  public readonly isLoadingCatalogBooks = signal(false);
  public readonly isLoadingIsbnBooks = signal(false);
  public readonly isSubmitting = signal(false);
  public readonly wizardSteps = CREATE_LOAN_WIZARD_STEPS;
  public readonly currentStep = signal<CreateLoanWizardStep>(1);

  public readonly maxReachableStep = computed(() => {
    if (this.selectedUser() === null) {
      return 1;
    }

    if (this.selectedBooks().length === 0) {
      return 2;
    }

    return CREATE_LOAN_WIZARD_STEPS.length;
  });

  public readonly searchFirstname = signal(
    this.loadFromSession('searchFirstname', ''),
  );
  public readonly searchLastname = signal(
    this.loadFromSession('searchLastname', ''),
  );
  public readonly bookSearchQuery = signal(
    this.loadFromSession('bookSearchQuery', ''),
  );
  public readonly bookSearchIsbn = signal(
    this.loadFromSession('bookSearchIsbn', ''),
  );
  public readonly users = signal<AuthUserDto[]>([]);
  public readonly schools = signal<School[]>([]);
  public readonly isExternalUserSearch = signal(
    this.loadFromSession('isExternalUserSearch', false),
  );
  public readonly selectedExternalSchoolId = signal<number | null>(
    this.loadFromSession('selectedExternalSchoolId', null),
  );

  public readonly catalogBooks = signal<Book[]>([]);
  public readonly isbnBooks = signal<Book[]>([]);
  public readonly selectedUser = signal<AuthUserDto | null>(
    this.loadFromSession('selectedUser', null),
  );
  public readonly selectedBooks = signal<Book[]>(
    this.loadFromSession('selectedBooks', []),
  );
  public readonly selectedCopyIdsByBook = signal<Record<number, number[]>>(
    this.loadFromSession('selectedCopyIdsByBook', {}),
  );
  public readonly scannedBooks = signal<Book[]>(
    this.loadFromSession('scannedBooks', []),
  );
  public readonly schoolSummary = signal<SchoolSummaryDto | null>(
    this.loadFromSession('schoolSummary', null),
  );
  public readonly isCustomDate = signal(
    this.loadFromSession('isCustomDate', false),
  );
  public readonly minReturnDate = signal(this.buildMinReturnDate());

  public readonly selectedCampusId = computed(() => this.getSelectedCampusId());
  public readonly availableExternalSchools = computed(() => {
    const currentSchoolId =
      this.authService.currentUser()?.school?.id ??
      this.authService.currentUser()?.schoolId ??
      null;
    return this.schools().filter((school) => school.id !== currentSchoolId);
  });

  private loadFromSession<T>(key: string, defaultValue: T): T {
    if (typeof sessionStorage === 'undefined') return defaultValue;
    try {
      const stored = sessionStorage.getItem(`create_loan_${key}`);
      return stored ? JSON.parse(stored) : defaultValue;
    } catch {
      return defaultValue;
    }
  }

  private saveToSession(key: string, value: any): void {
    if (typeof sessionStorage === 'undefined') return;
    sessionStorage.setItem(`create_loan_${key}`, JSON.stringify(value));
  }

  private clearSession(): void {
    if (typeof sessionStorage === 'undefined') return;
    const keys = [
      'selectedUser',
      'selectedBooks',
      'selectedCopyIdsByBook',
      'scannedBooks',
      'searchFirstname',
      'searchLastname',
      'bookSearchQuery',
      'bookSearchIsbn',
      'schoolSummary',
      'isExternalUserSearch',
      'selectedExternalSchoolId',
      'isCustomDate',
      'returnDate',
    ];
    keys.forEach((k) => sessionStorage.removeItem(`create_loan_${k}`));
  }

  public clearAllProgress(): void {
    this.selectedUser.set(null);
    this.selectedBooks.set([]);
    this.selectedCopyIdsByBook.set({});
    this.scannedBooks.set([]);
    this.users.set([]);
    this.catalogBooks.set([]);
    this.isbnBooks.set([]);
    this.searchFirstname.set('');
    this.searchLastname.set('');
    this.bookSearchQuery.set('');
    this.bookSearchIsbn.set('');
    this.schoolSummary.set(null);
    this.isExternalUserSearch.set(false);
    this.selectedExternalSchoolId.set(null);
    this.isCustomDate.set(false);
    this.loanForm.reset({ returnDate: '' });
    this.clearSession();
  }

  public readonly hasValidRefreshToken = signal<boolean | null>(null);
  public readonly showNoTokenWarningModal = signal(false);
  public readonly showClearProgressModal = signal(false);

  public requestClearAllProgress(): void {
    this.showClearProgressModal.set(true);
  }

  public confirmClearAllProgress(): void {
    this.showClearProgressModal.set(false);
    this.clearAllProgress();
  }

  public cancelClearProgress(): void {
    this.showClearProgressModal.set(false);
  }

  protected goBack(): void {
    this.router.navigate(['/books-management']);
  }

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
    returnDate: [
      this.loadFromSession<string>('returnDate', ''),
      Validators.required,
    ],
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

    effect(() => {
      this.saveToSession('selectedUser', this.selectedUser());
      this.saveToSession('selectedBooks', this.selectedBooks());
      this.saveToSession('selectedCopyIdsByBook', this.selectedCopyIdsByBook());
      this.saveToSession('scannedBooks', this.scannedBooks());
      this.saveToSession('searchFirstname', this.searchFirstname());
      this.saveToSession('searchLastname', this.searchLastname());
      this.saveToSession('bookSearchQuery', this.bookSearchQuery());
      this.saveToSession('bookSearchIsbn', this.bookSearchIsbn());
      this.saveToSession('schoolSummary', this.schoolSummary());
      this.saveToSession('isExternalUserSearch', this.isExternalUserSearch());
      this.saveToSession(
        'selectedExternalSchoolId',
        this.selectedExternalSchoolId(),
      );
      this.saveToSession('isCustomDate', this.isCustomDate());
    });

    this.loadSchools();

    this.loanForm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((val) => {
        this.saveToSession('returnDate', val.returnDate);
      });

    if (this.selectedUser()) {
      const user = this.selectedUser()!;
      this.hasValidRefreshToken.set(true);

      if (this.bookSearchQuery().trim()) {
        this.triggerCatalogSearch();
      }
      if (this.bookSearchIsbn().trim()) {
        this.triggerIsbnSearch();
      }
    }

    if (this.searchFirstname().trim() || this.searchLastname().trim()) {
      this.searchUsers();
    }
  }

  public readonly filteredBooks = computed(() => {
    const selectedBookIds = new Set(
      this.selectedBooks().map((book) => book.id),
    );
    const mergedBooksByKey = new Map<string, Book>();

    [...this.catalogBooks(), ...this.isbnBooks()].forEach((book) => {
      const key = book.id !== undefined ? `id-${book.id}` : `isbn-${book.isbn}`;
      if (!mergedBooksByKey.has(key)) {
        mergedBooksByKey.set(key, book);
      }
    });

    return Array.from(mergedBooksByKey.values()).filter((book) => {
      const hasId = book.id !== undefined;
      const hasCopiesAvailable = (book.availableCopies ?? 0) > 0;

      return hasId && hasCopiesAvailable;
    });
  });

  public readonly selectedBookIds = computed(() => {
    const ids = this.selectedBooks()
      .map((book) => book.id)
      .filter((bookId): bookId is number => bookId !== undefined);

    return Array.from(new Set(ids));
  });

  public readonly selectedBookCounts = computed(() => {
    return this.selectedBooks().reduce<Record<number, number>>(
      (counts, book) => {
        if (book.id === undefined) {
          return counts;
        }

        counts[book.id] = (counts[book.id] ?? 0) + 1;
        return counts;
      },
      {},
    );
  });

  public onFirstnameSearchChange(value: string): void {
    this.searchFirstname.set(value);
  }

  public onLastnameSearchChange(value: string): void {
    this.searchLastname.set(value);
  }

  public onExternalModeChange(isExternal: boolean): void {
    if (this.isExternalUserSearch() === isExternal) {
      return;
    }

    this.isExternalUserSearch.set(isExternal);
    this.clearSelectedUser();
    this.users.set([]);

    if (!isExternal) {
      this.selectedExternalSchoolId.set(null);
    }

    this.searchUsers();
  }

  public onExternalSchoolChange(schoolId: number | null): void {
    this.selectedExternalSchoolId.set(schoolId);
    this.clearSelectedUser();
    this.users.set([]);
    this.searchUsers();
  }

  public searchUsers(): void {
    const firstName = this.searchFirstname().trim();
    const lastName = this.searchLastname().trim();
    this.loadUsers(firstName, lastName);
  }

  public selectUser(user: AuthUserDto): void {
    this.selectedUser.set(user);
    this.schoolSummary.set(user.school ?? null);
    this.hasValidRefreshToken.set(null);
    this.updateDefaultReturnDate();
    this.triggerCatalogSearch();
    this.triggerIsbnSearch();

    this.hasValidRefreshToken.set(true);
  }

  public clearSelectedUser(): void {
    this.selectedUser.set(null);
    this.catalogBooks.set([]);
    this.isbnBooks.set([]);
    this.bookSearchQuery.set('');
    this.bookSearchIsbn.set('');
    this.hasValidRefreshToken.set(null);
    this.currentStep.set(1);
  }

  public onBookSearchQueryChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.bookSearchQuery.set(target.value);
    this.triggerCatalogSearch();
  }

  public onBookIsbnSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.bookSearchIsbn.set(target.value);
    this.triggerIsbnSearch();
  }

  public addBook(book: Book): void {
    if (book.id === undefined || (book.availableCopies ?? 0) < 1) {
      return;
    }

    this.bookService.getBookById(book.id).subscribe({
      next: (detailedBook) => this.addBookCopySelection(detailedBook),
      error: () =>
        this.showError('Exemplaren konden niet geladen worden voor dit boek.'),
    });
  }

  private addBookCopySelection(book: Book): void {
    if (book.id === undefined) {
      return;
    }

    const selectedCount = this.selectedBooks().filter(
      (selectedBook) => selectedBook.id === book.id,
    ).length;
    const selectedCampusId = this.getSelectedCampusId();
    const allAvailableCopies = this.getAvailableCopies(book);
    const availableCopies =
      selectedCampusId === null
        ? allAvailableCopies
        : allAvailableCopies.filter(
            (copy) => this.getCopyCampusId(book, copy.id) === selectedCampusId,
          );

    if (selectedCount >= allAvailableCopies.length) {
      this.showError(
        'Er zijn niet genoeg beschikbare exemplaren voor dit boek.',
      );
      return;
    }

    if (availableCopies.length === 0) {
      this.showError(
        'Je kan per lening alleen exemplaren uit dezelfde campus selecteren. Maak een aparte lening voor een andere campus.',
      );
      return;
    }

    const selectedCopyIds = this.selectedCopyIdsByBook()[book.id] ?? [];
    const nextCopy = availableCopies.find(
      (copy) => !selectedCopyIds.includes(copy.id),
    );

    if (!nextCopy) {
      this.showError('Er is geen beschikbaar exemplaar om te selecteren.');
      return;
    }

    this.selectedCopyIdsByBook.update((current) => ({
      ...current,
      [book.id!]: [...selectedCopyIds, nextCopy.id],
    }));
    this.selectedBooks.update((books) => [...books, book]);
  }

  public removeBook(bookId: number | undefined): void {
    if (bookId === undefined) {
      return;
    }

    let removed = false;
    this.selectedBooks.update((books) =>
      books.filter((book) => {
        if (removed || book.id !== bookId) {
          return true;
        }

        removed = true;
        return false;
      }),
    );

    this.selectedCopyIdsByBook.update((current) => {
      const next = { ...current };
      const copyIds = [...(next[bookId] ?? [])];
      copyIds.pop();

      if (copyIds.length === 0) {
        delete next[bookId];
      } else {
        next[bookId] = copyIds;
      }

      return next;
    });
  }

  public updateSelectedCopy(
    bookId: number,
    index: number,
    copyId: number,
  ): void {
    const book = this.selectedBooks().find(
      (selectedBook) => selectedBook.id === bookId,
    );
    if (
      book &&
      !this.canSelectCopyForCurrentLoan(book, copyId, bookId, index)
    ) {
      this.showError(
        'Je kan per lening alleen exemplaren uit dezelfde campus selecteren. Maak een aparte lening voor een andere campus.',
      );
      return;
    }

    this.selectedCopyIdsByBook.update((current) => {
      const copyIds = [...(current[bookId] ?? [])];
      copyIds[index] = copyId;

      return {
        ...current,
        [bookId]: copyIds,
      };
    });
  }

  public createLoan(): void {
    const user = this.selectedUser();
    const books = this.selectedBooks();
    const returnDate = this.loanForm.controls.returnDate.value;

    if (!user || books.length === 0 || !returnDate) {
      this.showError(
        'Vul alle verplichte velden in voor je een lening aanmaakt.',
      );
      return;
    }

    if (returnDate < this.minReturnDate()) {
      this.showError(
        'De terugbrengdatum moet minimaal één dag in de toekomst liggen.',
      );
      return;
    }

    const hasInvalidBook = books.some((book) => book.id === undefined);
    if (hasInvalidBook) {
      this.showError('Eén of meerdere geselecteerde boeken zijn ongeldig.');
      return;
    }

    const booksPayload = Object.entries(this.selectedBookCounts()).map(
      ([id, count]) => ({
        id: Number(id),
        bookCopyIds: this.selectedCopyIdsByBook()[Number(id)] ?? [],
        copiesCount: count,
      }),
    );

    if (booksPayload.length === 0) {
      this.showError('Er zijn geen geldige boeken geselecteerd.');
      return;
    }

    const bookCopyIds = Object.values(this.selectedCopyIdsByBook()).flat();

    if (bookCopyIds.length !== books.length) {
      this.showError('Kies voor elk boek een specifiek exemplaar.');
      return;
    }

    if (!this.hasSingleSelectedCampus()) {
      this.showError(
        'Je kan per lening alleen exemplaren uit dezelfde campus selecteren. Maak een aparte lening voor een andere campus.',
      );
      return;
    }

    if (this.hasValidRefreshToken() === false) {
      this.showNoTokenWarningModal.set(true);
      return;
    }

    this.submitLoan(user.id, bookCopyIds, booksPayload, returnDate);
  }

  public confirmLoanWithWarning(): void {
    this.showNoTokenWarningModal.set(false);

    const user = this.selectedUser();
    const booksPayload = Object.entries(this.selectedBookCounts()).map(
      ([id, count]) => ({
        id: Number(id),
        bookCopyIds: this.selectedCopyIdsByBook()[Number(id)] ?? [],
        copiesCount: count,
      }),
    );
    const bookCopyIds = Object.values(this.selectedCopyIdsByBook()).flat();
    const returnDate = this.loanForm.controls.returnDate.value;

    if (user && returnDate) {
      if (!this.hasSingleSelectedCampus()) {
        this.showError(
          'Je kan per lening alleen exemplaren uit dezelfde campus selecteren. Maak een aparte lening voor een andere campus.',
        );
        return;
      }

      this.submitLoan(user.id, bookCopyIds, booksPayload, returnDate);
    }
  }

  public cancelLoanWarning(): void {
    this.showNoTokenWarningModal.set(false);
  }

  private submitLoan(
    userId: string,
    bookCopyIds: number[],
    booksPayload: any[],
    returnDate: string,
  ): void {
    this.isSubmitting.set(true);

    this.loansService
      .createLoans({
        id: 0,
        userId: userId,
        bookCopyIds: bookCopyIds,
        books: booksPayload,
        returnDate: toLocalDate(new Date(returnDate)),
      })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          const count = bookCopyIds.length;
          this.showSuccess(
            count === 1
              ? 'De lening is succesvol aangemaakt.'
              : 'De leningen zijn succesvol aangemaakt.',
          );
          this.clearAllProgress();
          void this.router.navigate(['/books-management']);
        },
        error: () => {
          this.isSubmitting.set(false);
          this.showError(
            'De lening kon niet worden aangemaakt. Probeer opnieuw.',
          );
        },
      });
  }

  private getAvailableCopies(book: Book): BookCopyLocation[] {
    return (book.copyLocations ?? [])
      .flatMap((location) => location.copies)
      .filter(
        (copy) =>
          copy.availability === 'AVAILABLE' && copy.condition !== 'LOST',
      );
  }

  private getCopyCampusId(book: Book, copyId: number): number | null {
    for (const location of book.copyLocations ?? []) {
      if (location.copies.some((copy) => copy.id === copyId)) {
        return location.campusId;
      }
    }

    return null;
  }

  private canSelectCopyForCurrentLoan(
    book: Book,
    copyId: number,
    bookIdToReplace?: number,
    indexToReplace?: number,
  ): boolean {
    const copyCampusId = this.getCopyCampusId(book, copyId);
    if (copyCampusId === null) {
      return false;
    }

    for (const selectedBook of this.selectedBooks()) {
      if (selectedBook.id === undefined) {
        continue;
      }

      const selectedCopyIds =
        this.selectedCopyIdsByBook()[selectedBook.id] ?? [];
      for (let index = 0; index < selectedCopyIds.length; index++) {
        if (selectedBook.id === bookIdToReplace && index === indexToReplace) {
          continue;
        }

        const selectedCampusId = this.getCopyCampusId(
          selectedBook,
          selectedCopyIds[index],
        );
        if (selectedCampusId !== null && selectedCampusId !== copyCampusId) {
          return false;
        }
      }
    }

    return true;
  }

  private getSelectedCampusId(): number | null {
    for (const selectedBook of this.selectedBooks()) {
      if (selectedBook.id === undefined) {
        continue;
      }

      for (const copyId of this.selectedCopyIdsByBook()[selectedBook.id] ??
        []) {
        const campusId = this.getCopyCampusId(selectedBook, copyId);
        if (campusId !== null) {
          return campusId;
        }
      }
    }

    return null;
  }

  private hasSingleSelectedCampus(): boolean {
    const campusIds = new Set<number>();

    for (const selectedBook of this.selectedBooks()) {
      if (selectedBook.id === undefined) {
        continue;
      }

      for (const copyId of this.selectedCopyIdsByBook()[selectedBook.id] ??
        []) {
        const campusId = this.getCopyCampusId(selectedBook, copyId);
        if (campusId !== null) {
          campusIds.add(campusId);
        }
      }
    }

    return campusIds.size <= 1;
  }

  private loadUsers(firstName: string = '', lastName: string = ''): void {
    if (this.isExternalUserSearch()) {
      this.loadExternalUsers(firstName, lastName);
      return;
    }

    this.isLoadingUsers.set(true);

    this.userService
      .getUsers({ WithfirstName: firstName, WithlastName: lastName })
      .subscribe({
        next: (users) => {
          this.users.set(users);
          this.isLoadingUsers.set(false);
          if (users.length === 1) {
            this.selectUser(users[0]);
          }
        },
        error: (err: any) => {
          this.users.set([]);
          this.isLoadingUsers.set(false);
          this.showError('Gebruikers konden niet worden geladen.');
          console.log('Error loading users:', err);
          console.log('Error loading users with parameters:', {
            firstName,
            lastName,
          });
        },
      });
  }

  private loadExternalUsers(
    firstName: string = '',
    lastName: string = '',
  ): void {
    const schoolId = this.selectedExternalSchoolId();
    if (schoolId === null) {
      this.users.set([]);
      this.isLoadingUsers.set(false);
      return;
    }

    this.isLoadingUsers.set(true);

    this.userService
      .searchExternalUsers(schoolId, firstName, lastName)
      .subscribe({
        next: (users) => {
          this.users.set(users);
          this.isLoadingUsers.set(false);
          if (users.length === 1) {
            this.selectUser(users[0]);
          }
        },
        error: () => {
          this.users.set([]);
          this.isLoadingUsers.set(false);
          this.showError('Externe gebruikers konden niet worden geladen.');
        },
      });
  }

  private loadSchools(): void {
    this.isLoadingSchools.set(true);

    this.schoolService.getAll().subscribe({
      next: (schools) => {
        this.schools.set(schools);
        this.isLoadingSchools.set(false);
      },
      error: () => {
        this.schools.set([]);
        this.isLoadingSchools.set(false);
        this.showError('Scholen konden niet worden geladen.');
      },
    });
  }

  private loadCatalogBooks(searchTerm: string = ''): void {
    if (!this.selectedUser()) {
      this.catalogBooks.set([]);
      return;
    }

    this.isLoadingCatalogBooks.set(true);

    this.bookService
      .getFilteredBooks(
        searchTerm.length > 0 ? searchTerm : null,
        'AVAILABLE',
        null,
        null,
        null,
        null,
        'ALL',
        0,
        50,
        'AZ',
      )
      .subscribe({
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
          const wasAdded = this.selectScannedBook(book);
          if (wasAdded) {
            this.scannedBooks.update((entries) => [...entries, book]);
          }
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

  public handleScannedIsbn(isbn: string): void {
    const cleanedIsbn = isbn.trim();
    if (!cleanedIsbn) {
      return;
    }

    this.fetchBookByIsbn(cleanedIsbn, true);
  }

  private selectScannedBook(book: Book): boolean {
    if (book.id === undefined) {
      return false;
    }

    const hasCopiesAvailable = (book.availableCopies ?? 0) > 0;
    if (!hasCopiesAvailable) {
      this.showError('Het gescande boek is niet beschikbaar voor uitlening.');
      return false;
    }

    this.addBook(book);
    this.showSuccess('Boek succesvol gescand en toegevoegd.');
    return true;
  }

  public removeLastScannedBook(): void {
    const scanned = this.scannedBooks();
    if (scanned.length === 0) {
      return;
    }

    const last = scanned[scanned.length - 1];
    this.scannedBooks.set(scanned.slice(0, -1));

    if (last.id !== undefined) {
      this.removeBook(last.id);
    }
  }

  private triggerCatalogSearch(): void {
    this.catalogSearchSubject.next(this.bookSearchQuery().trim());
  }

  private triggerIsbnSearch(isbnOverride?: string): void {
    this.isbnSearchSubject.next(isbnOverride ?? this.bookSearchIsbn().trim());
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

  private updateDefaultReturnDate(): void {
    if (this.isCustomDate()) {
      return;
    }
    const school = this.schoolSummary();
    const defaultDays = school?.defaultLoanDurationDays ?? 15;
    const today = new Date();
    today.setDate(today.getDate() + defaultDays);
    this.loanForm.controls.returnDate.setValue(toLocalDate(today));
  }

  public onCustomDate(): void {
    this.isCustomDate.set(true);
    setTimeout(() => {
      const input = document.getElementById(
        'returnDate',
      ) as HTMLInputElement | null;
      input?.focus();
      input?.showPicker?.();
    });
  }

  public onUseDefaultDate(): void {
    this.isCustomDate.set(false);
    this.updateDefaultReturnDate();
  }

  public onChangeDefaultDays(): void {
    const currentDays = this.schoolSummary()?.defaultLoanDurationDays ?? 15;
    const input = window.prompt(
      'Wijzig de standaard uitleentermijn (in dagen):',
      currentDays.toString(),
    );

    if (input === null) {
      return;
    }

    const newDays = parseInt(input, 10);
    if (isNaN(newDays) || newDays < 1 || newDays > 365) {
      this.showError('Voer een geldig aantal dagen in (1-365).');
      return;
    }

    const school = this.schoolSummary();
    if (!school?.id) {
      this.showError('Geen school gevonden om bij te werken.');
      return;
    }

    this.updateSchoolDefaultDays(school.id, newDays);
  }

  private updateSchoolDefaultDays(schoolId: number, newDays: number): void {
    // TODO: Replace with actual API call to update school settings
    // For now, update the local signal
    const currentSummary = this.schoolSummary();
    if (currentSummary) {
      this.schoolSummary.set({
        ...currentSummary,
        defaultLoanDurationDays: newDays,
      });
      this.updateDefaultReturnDate();
      this.showSuccess(
        `Standaard uitleentermijn gewijzigd naar ${newDays} dagen.`,
      );
    }
  }

  public setCurrentStep(step: number): void {
    if (!this.isCreateLoanWizardStep(step)) {
      return;
    }

    this.currentStep.set(step);
  }

  private isCreateLoanWizardStep(step: number): step is CreateLoanWizardStep {
    return step >= 1 && step <= CREATE_LOAN_WIZARD_STEPS.length;
  }

  public goToNextStep(): void {
    const nextStep = this.currentStep() + 1;

    if (!this.isCreateLoanWizardStep(nextStep)) {
      return;
    }

    if (nextStep > this.maxReachableStep()) {
      return;
    }

    this.currentStep.set(nextStep);
  }

  public goToPreviousStep(): void {
    const previousStep = this.currentStep() - 1;

    if (!this.isCreateLoanWizardStep(previousStep)) {
      return;
    }

    this.currentStep.set(previousStep);
  }
}
