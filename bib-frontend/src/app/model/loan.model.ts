import { LocalDate } from '../shared/types/local-date.type';
import { PaginatedResponse } from './shared/paging/PaginatedResponse.model';

export type LoanStatus = 'ACTIVE' | 'OVERDUE' | 'RETURNED';

export interface LoanBook {
  id: number;
  title?: string;
  author?: string;
  isbn?: string;
  imageUrl?: string;
  copiesCount?: number;
}

export interface Loan {
  id?: number;
  userId: string;
  userDisplayName?: string;
  bookIds: number[];
  books?: LoanBook[];
  loanDate?: LocalDate;
  returnDate: LocalDate;
  returnedDate?: LocalDate | null;
  status?: LoanStatus;
  extensionCount?: number;
}

export interface LoanHistory {
  activeLoanCount: number;
  returnedLoanCount: number;
  totalLoanedBookCount: number;
  activeLoans: Loan[];
  returnedLoans: PaginatedResponse<Loan>;
}
