import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { BookService } from '../../../services/bookService/book.service';
import { LoansService } from '../../../services/loan/loans.service';
import { UserService } from '../../../services/user/user.service';
import { SchoolService } from '../../../services/school/school.service';
import { AuthService } from '../../../services/authorization/auth.service';
import { ToastService } from '../../../services/toast/toast.service';
import { AuthUserDto } from '../../../model/auth.model';
import { Book } from '../../../model/book/book.model';
import { LocalDate } from '../../../shared/types/local-date.type';

import { CreateLoanComponent } from './create-loan.component';

describe('CreateLoanComponent', () => {
  let component: CreateLoanComponent;
  let fixture: ComponentFixture<CreateLoanComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let loansServiceSpy: jasmine.SpyObj<LoansService>;
  let schoolServiceSpy: jasmine.SpyObj<SchoolService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  let currentUser: WritableSignal<AuthUserDto | null>;

  const book: Book = {
    id: 1,
    title: 'Test Book',
    author: 'Test Author',
    pages: 100,
    releaseYear: 2026,
    didactic: false,
    availableCopies: 2,
    copyLocations: [
      {
        campusId: 1,
        campusName: 'Campus 1',
        copies: [
          { id: 10, availability: 'AVAILABLE', condition: 'GOOD_CONDITION' },
          { id: 11, availability: 'AVAILABLE', condition: 'GOOD_CONDITION' },
        ],
      },
    ],
  };

  beforeEach(async () => {
    sessionStorage.clear();

    userServiceSpy = jasmine.createSpyObj<UserService>('UserService', ['getUsers']);
    bookServiceSpy = jasmine.createSpyObj<BookService>('BookService', [
      'getFilteredBooks',
      'getBookById',
      'getBookByISBN',
    ]);
    loansServiceSpy = jasmine.createSpyObj<LoansService>('LoansService', [
      'createLoans',
    ]);
    schoolServiceSpy = jasmine.createSpyObj<SchoolService>('SchoolService', ['getAll']);
    toastServiceSpy = jasmine.createSpyObj<ToastService>('ToastService', ['show']);

    userServiceSpy.getUsers.and.returnValue(of([]));
    bookServiceSpy.getFilteredBooks.and.returnValue(
      of({
        data: [],
        meta: { currentPage: 0, pageSize: 50, totalItems: 0, totalPages: 0 },
      }),
    );
    bookServiceSpy.getBookById.and.returnValue(of(book));
    bookServiceSpy.getBookByISBN.and.returnValue(of(book));
    loansServiceSpy.createLoans.and.returnValue(of([]));
    schoolServiceSpy.getAll.and.returnValue(of([]));
    currentUser = signal<AuthUserDto | null>(null);

    await TestBed.configureTestingModule({
      imports: [CreateLoanComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: LoansService, useValue: loansServiceSpy },
        { provide: SchoolService, useValue: schoolServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: AuthService, useValue: { currentUser } },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateLoanComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('Given_CopyAlreadySelected_When_UpdateSelectedCopy_Then_KeepsExistingSelection', () => {
    // Arrange
    component.selectedBooks.set([book, book]);
    component.selectedCopyIdsByBook.set({ 1: [10, 11] });

    // Act
    component.updateSelectedCopy(1, 1, 10);

    // Assert
    expect(component.selectedCopyIdsByBook()[1]).toEqual([10, 11]);
    expect(toastServiceSpy.show).toHaveBeenCalledWith(
      'Elk te lenen exemplaar moet uniek zijn. Kies een ander exemplaar.',
      'error',
    );
  });

  it('Given_DuplicateCopySelection_When_CreateLoan_Then_DoesNotSubmitLoan', () => {
    // Arrange
    component.selectedUser.set({ id: 'user-1', role: 'STUDENT' });
    component.selectedBooks.set([book, book]);
    component.selectedCopyIdsByBook.set({ 1: [10, 10] });
    (component as any).loanForm.controls.returnDate.setValue(
      '2026-06-10' as LocalDate,
    );

    // Act
    component.createLoan();

    // Assert
    expect(loansServiceSpy.createLoans).not.toHaveBeenCalled();
    expect(toastServiceSpy.show).toHaveBeenCalledWith(
      'Elk te lenen exemplaar moet uniek zijn. Kies een ander exemplaar.',
      'error',
    );
  });

  it('Given_CurrentSchool_When_AddBook_Then_FetchesBookByIdWithSchoolId', () => {
    // Arrange
    currentUser.set({ id: 'librarian-1', role: 'LIBRARIAN', schoolId: 7 });

    // Act
    component.addBook(book);

    // Assert
    expect(bookServiceSpy.getBookById).toHaveBeenCalledWith(1, 7);
  });

  it('Given_CurrentSchool_When_LoadCatalogBooks_Then_SearchUsesSchoolId', () => {
    // Arrange
    currentUser.set({ id: 'librarian-1', role: 'LIBRARIAN', schoolId: 7 });
    component.selectedUser.set({ id: 'user-1', role: 'STUDENT' });

    // Act
    (component as any).loadCatalogBooks('harry');

    // Assert
    expect(bookServiceSpy.getFilteredBooks).toHaveBeenCalledWith(
      'harry',
      'AVAILABLE',
      null,
      null,
      null,
      null,
      'ALL',
      0,
      50,
      'AZ',
      7,
    );
  });

  it('Given_CurrentSchool_When_FetchBookByIsbn_Then_SearchUsesSchoolId', () => {
    // Arrange
    currentUser.set({ id: 'librarian-1', role: 'LIBRARIAN', schoolId: 7 });
    component.selectedUser.set({ id: 'user-1', role: 'STUDENT' });

    // Act
    (component as any).fetchBookByIsbn('9781234567890', false);

    // Assert
    expect(bookServiceSpy.getBookByISBN).toHaveBeenCalledWith('9781234567890', 7);
  });
});
