import { Loan } from './loan.model';

export type LoanGroupStatus = 'ACTIVE' | 'OVERDUE' | 'RETURNED';

export interface LoanGroup {
  groupId: string;
  userId: string;
  userDisplayName: string;
  groupStatus: LoanGroupStatus;
  loanCount: number;
  bookCount: number;
  earliestReturnDate: string | null;
  loans: Loan[];
}