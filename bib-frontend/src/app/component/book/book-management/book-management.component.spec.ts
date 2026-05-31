import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { BookManagementComponent } from './book-management.component';
import { LoansService } from '../../../services/loan/loans.service';
import { ToastService } from '../../../services/toast/toast.service';
import { LoanGroup } from '../../../model/loan/loanGroup.model';
import { LocalDate } from '../../../shared/types/local-date.type';

describe('BookManagementComponent', () => {
  let component: BookManagementComponent;
  let fixture: ComponentFixture<BookManagementComponent>;
  let loansService: jasmine.SpyObj<LoansService>;

  const emptyLoanResponse = {
    data: [],
    meta: {
      currentPage: 0,
      pageSize: 10,
      totalItems: 0,
      totalPages: 0,
    },
  };

  beforeEach(async () => {
    loansService = jasmine.createSpyObj<LoansService>('LoansService', [
      'getGroupedLoans',
      'getLoanStats',
    ]);
    loansService.getGroupedLoans.and.returnValue(of(emptyLoanResponse));
    loansService.getLoanStats.and.returnValue(
      of({ activeLoans: 0, overdueLoans: 0, returnedLoans: 0 }),
    );

    await TestBed.configureTestingModule({
      imports: [BookManagementComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LoansService, useValue: loansService },
        {
          provide: ToastService,
          useValue: jasmine.createSpyObj<ToastService>('ToastService', ['show']),
        },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(BookManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('Given_ReturnedLoan_When_GetDisplayDate_Then_ReturnsReturnedDate', () => {
    // Arrange
    const loan = {
      id: 1,
      userId: 'user-1',
      returnDate: '2026-06-10' as LocalDate,
      returnedDate: '2026-06-03' as LocalDate,
    };

    // Act
    const result = component.getDisplayDate(loan);

    // Assert
    expect(result).toBe('2026-06-03');
  });

  it('Given_ActiveLoan_When_GetDisplayDate_Then_ReturnsReturnDate', () => {
    // Arrange
    const loan = {
      id: 1,
      userId: 'user-1',
      returnDate: '2026-06-10' as LocalDate,
      returnedDate: null,
    };

    // Act
    const result = component.getDisplayDate(loan);

    // Assert
    expect(result).toBe('2026-06-10');
  });

  it('Given_ReturnedLoanGroup_When_GetGroupDisplayDate_Then_ReturnsLatestReturnedDate', () => {
    // Arrange
    const group: LoanGroup = {
      groupId: 'user-1-RETURNED',
      userId: 'user-1',
      userDisplayName: 'Jane Doe',
      groupStatus: 'RETURNED',
      loanCount: 2,
      bookCount: 2,
      earliestReturnDate: null,
      loans: [
        {
          id: 1,
          userId: 'user-1',
          returnDate: '2026-06-10' as LocalDate,
          returnedDate: '2026-06-03' as LocalDate,
        },
        {
          id: 2,
          userId: 'user-1',
          returnDate: '2026-06-12' as LocalDate,
          returnedDate: '2026-06-05' as LocalDate,
        },
      ],
    };

    // Act
    const result = component.getGroupDisplayDate(group);

    // Assert
    expect(result).toBe('2026-06-05');
  });

  it('Given_ActiveLoanGroup_When_GetGroupDisplayDate_Then_ReturnsEarliestReturnDate', () => {
    // Arrange
    const group: LoanGroup = {
      groupId: 'user-1-ACTIVE',
      userId: 'user-1',
      userDisplayName: 'Jane Doe',
      groupStatus: 'ACTIVE',
      loanCount: 1,
      bookCount: 1,
      earliestReturnDate: '2026-06-10',
      loans: [
        {
          id: 1,
          userId: 'user-1',
          returnDate: '2026-06-10' as LocalDate,
          returnedDate: null,
        },
      ],
    };

    // Act
    const result = component.getGroupDisplayDate(group);

    // Assert
    expect(result).toBe('2026-06-10');
  });
});
