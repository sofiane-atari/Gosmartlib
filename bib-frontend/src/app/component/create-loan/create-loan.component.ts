import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Book } from '../../model/book.model';
import { User } from '../../model/user.model';
import { BookService } from '../../services/BookService/book.service';
import { LoansService } from '../../services/loans.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-create-loan',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './create-loan.component.html',
  styleUrl: './create-loan.component.css',
})
export class CreateLoanComponent {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly bookService = inject(BookService);
  private readonly loansService = inject(LoansService);

  public readonly isLoadingUsers = signal(false);
  public readonly isLoadingBooks = signal(false);
  public readonly isSubmitting = signal(false);
  public readonly errorMessage = signal<string>('');
  public readonly successMessage = signal<string>('');

  public readonly searchFirstname = signal('');
  public readonly searchLastname = signal('');
  public readonly bookSearch = signal('');
  public readonly users = signal<User[]>([]);
  public readonly books = signal<Book[]>([]);
  public readonly selectedUser = signal<User | null>(null);
  public readonly selectedBooks = signal<Book[]>([]);

  protected loanForm = this.fb.group({
    returnDate: ['', Validators.required],
  });

  public readonly filteredBooks = computed(() => {
    const query = this.bookSearch().trim().toLowerCase();
    const selectedBookIds = new Set(this.selectedBooks().map((book) => book.id));

    return this.books().filter((book) => {
      const hasId = book.id !== undefined;
      const notAlreadySelected = !selectedBookIds.has(book.id);
      const hasCopiesAvailable = (book.availableCopies ?? 0) > 0;

      if (!query) {
        return hasId && notAlreadySelected && hasCopiesAvailable;
      }

      const inTitle = book.title.toLowerCase().includes(query);
      const inAuthor = book.author.toLowerCase().includes(query);
      const inIsbn = (book.ISBN ?? '').toLowerCase().includes(query);

      return (
        hasId &&
        notAlreadySelected &&
        hasCopiesAvailable &&
        (inTitle || inAuthor || inIsbn)
      );
    });
  });

  public constructor() {
    this.loadUsers();
    this.loadBooks();
  }

  public onFirstnameSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchFirstname.set(target.value);
  }

  public onLastnameSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchLastname.set(target.value);
  }

  public searchUsers(): void {
    const firstName = this.searchFirstname().trim();
    const lastName = this.searchLastname().trim();
    this.loadUsers(firstName, lastName);
  }

  public selectUser(user: User): void {
    this.selectedUser.set(user);
    this.selectedBooks.set([]);
    this.successMessage.set('');
    this.errorMessage.set('');
  }

  public clearSelectedUser(): void {
    this.selectedUser.set(null);
    this.selectedBooks.set([]);
    this.successMessage.set('');
    this.errorMessage.set('');
  }

  public onBookSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.bookSearch.set(target.value);
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
        },
        error: () => {
          this.isSubmitting.set(false);
          this.errorMessage.set('De lening kon niet aangemaakt worden. Probeer opnieuw.');
          this.successMessage.set('');
        },
      });
  }

  protected trackByUserId(index: number, user: User): number {
    return user.id ?? index;
  }

  protected trackByBookId(index: number, book: Book): number {
    return book.id ?? index;
  }

  private loadUsers(firstName: string = '', lastName: string = ''): void {
    this.isLoadingUsers.set(true);

    this.userService
      .getUsers({ WithfirstName: firstName, WithlastName: lastName })
      .subscribe({
        next: (users) => {
          this.users.set(users);
          this.isLoadingUsers.set(false);
        },
        error: () => {
          this.users.set([]);
          this.isLoadingUsers.set(false);
          this.errorMessage.set('Gebruikers konden niet geladen worden.');
        },
      });
  }

  private loadBooks(): void {
    this.isLoadingBooks.set(true);

    this.bookService.getBooks().subscribe({
      next: (books) => {
        this.books.set(books);
        this.isLoadingBooks.set(false);
      },
      error: () => {
        this.books.set([]);
        this.isLoadingBooks.set(false);
        this.errorMessage.set('Boeken konden niet geladen worden.');
      },
    });
  }
}