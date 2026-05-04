import { LocalDate } from '../shared/types/local-date.type';

export type LoanStatus = 'ACTIVE' | 'OVERDUE' | 'RETURNED';

export interface LoanBook {
  id: number;
  title: string;
  author?: string;
  isbn?: string;
  imageUrl?: string;
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
}

export interface LoanHistory {
  activeLoanCount: number;
  returnedLoanCount: number;
  totalLoanedBookCount: number;
  activeLoans: Loan[];
  returnedLoans: Loan[];
}
