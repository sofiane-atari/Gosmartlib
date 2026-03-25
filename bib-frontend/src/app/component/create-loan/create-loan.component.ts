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
  private readonly bookSearchSubject = new Subject<string>();

  public readonly isLoadingUsers = signal(false);
  public readonly isLoadingBooks = signal(false);
  public readonly isSubmitting = signal(false);
  public readonly errorMessage = signal<string>('');
  public readonly successMessage = signal<string>('');

  public readonly searchFirstname = signal('');
  public readonly searchLastname = signal('');
  // TODO: TEMPORARY - Remove this signal when searchId feature is no longer needed
  public readonly searchId = signal('');
  public readonly bookSearchTitle = signal('');
  public readonly bookSearchAuthor = signal('');
  public readonly bookSearchIsbn = signal('');
  public readonly users = signal<AuthUserDto[]>([]);
  public readonly books = signal<Book[]>([]);
  public readonly selectedUser = signal<AuthUserDto | null>(null);
  public readonly selectedBooks = signal<Book[]>([]);

  public readonly hasBookSearchInput = computed(() => {
    return (
      this.bookSearchTitle().trim().length > 0 ||
      this.bookSearchAuthor().trim().length > 0 ||
      this.bookSearchIsbn().trim().length > 0
    );
  });

  protected loanForm = this.fb.group({
    returnDate: ['', Validators.required],
  });

  public constructor() {
    this.bookSearchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((searchTerm: string) => {
        if (!this.selectedUser()) {
          return;
        }

        if (!this.hasBookSearchInput()) {
          this.books.set([]);
          return;
        }

        this.loadBooks(searchTerm);
      });
  }

  public readonly filteredBooks = computed(() => {
    const selectedBookIds = new Set(this.selectedBooks().map((book) => book.id));

    return this.books().filter((book) => {
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
    this.successMessage.set('');
    this.errorMessage.set('');
    this.triggerBookSearch();
  }

  public clearSelectedUser(): void {
    this.selectedUser.set(null);
    this.selectedBooks.set([]);
    this.books.set([]);
    this.bookSearchTitle.set('');
    this.bookSearchAuthor.set('');
    this.bookSearchIsbn.set('');
    this.successMessage.set('');
    this.errorMessage.set('');
  }

  public onBookTitleSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.bookSearchTitle.set(target.value);
    this.triggerBookSearch();
  }

  public onBookAuthorSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.bookSearchAuthor.set(target.value);
    this.triggerBookSearch();
  }

  public onBookIsbnSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.bookSearchIsbn.set(target.value);
    this.triggerBookSearch();
  }

  public addBook(book: Book): void {
    if (book.id === undefined || (book.availableCopies ?? 0) < 1) {
      return;
    }

    this.selectedBooks.update((books) => [...books, book]);
    this.successMessage.set('');
    this.errorMessage.set('');
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
      this.errorMessage.set('Vul alle verplichte velden in voor je een lening aanmaakt.');
      this.successMessage.set('');
      return;
    }

    const bookIds = books
      .map((book) => book.id)
      .filter((bookId): bookId is number => bookId !== undefined);

    if (bookIds.length !== books.length) {
      this.errorMessage.set('Eén of meerdere geselecteerde boeken zijn ongeldig.');
      this.successMessage.set('');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.loansService
      .createLoan({
        id: 0,
        userId: user.id,
        booksId: bookIds,
        returnDate: new Date(returnDate),
      })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.successMessage.set('De lening is succesvol aangemaakt.');
          this.selectedBooks.set([]);
          this.loanForm.reset({ returnDate: '' });
          this.triggerBookSearch();
        },
        error: () => {
          this.isSubmitting.set(false);
          this.errorMessage.set('De lening kon niet aangemaakt worden. Probeer opnieuw.');
          this.successMessage.set('');
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
          this.errorMessage.set('Gebruikers konden niet geladen worden.');
          console.log('Error loading users:', err);
          console.log('Error loading users with parameters:', { firstName, lastName, id });
        },
      });
  }

  private loadBooks(searchTerm: string = ''): void {
    if (!this.selectedUser()) {
      this.books.set([]);
      return;
    }

    this.isLoadingBooks.set(true);

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
        this.books.set(response.data);
        this.isLoadingBooks.set(false);
      },
      error: () => {
        this.books.set([]);
        this.isLoadingBooks.set(false);
        this.errorMessage.set('Boeken konden niet geladen worden.');
      },
    });
  }

  private triggerBookSearch(): void {
    const searchTerm = this.buildBookSearchTerm();
    this.bookSearchSubject.next(searchTerm);
  }

  private buildBookSearchTerm(): string {
    return [
      this.bookSearchTitle().trim(),
      this.bookSearchAuthor().trim(),
      this.bookSearchIsbn().trim(),
    ]
      .filter((value) => value.length > 0)
      .join(' ')
      .trim();
  }
}