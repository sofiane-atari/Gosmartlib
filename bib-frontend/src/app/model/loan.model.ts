import { LocalDate } from "../shared/types/local-date.type";

export interface LoanBook {
  id: number;
  title: string;
  author?: string;
  isbn?: string;
}

export interface Loan {
  id?: number;
  userId: string;
  userDisplayName?: string;
  bookIds: number[];
  books?: LoanBook[];
  loanDate?: LocalDate;
  returnDate: LocalDate;
}
