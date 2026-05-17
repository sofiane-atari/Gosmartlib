import { LocalDate } from '../shared/types/local-date.type';
import { BookCondition } from './book/book.model';

export type LoanStatus = 'ACTIVE' | 'OVERDUE' | 'RETURNED';

export interface LoanBook {
  id: number;
  title?: string;
  author?: string;
  isbn?: string;
  imageUrl?: string;
  bookCopyIds?: number[];
  copiesCount?: number;
}

export interface Loan {
  id?: number;
  userId: string;
  userDisplayName?: string;
  bookCopyIds?: number[];
  books?: LoanBook[];
  loanDate?: LocalDate;
  returnDate: LocalDate;
  returnedDate?: LocalDate | null;
  status?: LoanStatus;
  extensionCount?: number;
}

export interface LoanReturnRequest {
  copyConditions: {
    bookCopyId: number;
    condition: BookCondition;
  }[];
}

export interface LoanHistory {
  activeLoanCount: number;
  returnedLoanCount: number;
  totalLoanedBookCount: number;
  activeLoans: Loan[];
  returnedLoans: Loan[];
}
