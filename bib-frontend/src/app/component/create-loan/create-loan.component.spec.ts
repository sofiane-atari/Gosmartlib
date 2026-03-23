import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BookService } from '../../services/BookService/book.service';
import { LoansService } from '../../services/loans.service';
import { UserService } from '../../services/user.service';

import { CreateLoanComponent } from './create-loan.component';

describe('CreateLoanComponent', () => {
  let component: CreateLoanComponent;
  let fixture: ComponentFixture<CreateLoanComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let loansServiceSpy: jasmine.SpyObj<LoansService>;

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj<UserService>('UserService', ['getUsers']);
    bookServiceSpy = jasmine.createSpyObj<BookService>('BookService', ['getBooks']);
    loansServiceSpy = jasmine.createSpyObj<LoansService>('LoansService', ['createLoan']);

    userServiceSpy.getUsers.and.returnValue(of([]));
    bookServiceSpy.getBooks.and.returnValue(of([]));
    loansServiceSpy.createLoan.and.returnValue(
      of({ id: 1, userId: 1, booksId: [], returnDate: new Date() }),
    );

    await TestBed.configureTestingModule({
      imports: [CreateLoanComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: BookService, useValue: bookServiceSpy },
        { provide: LoansService, useValue: loansServiceSpy },
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
});
