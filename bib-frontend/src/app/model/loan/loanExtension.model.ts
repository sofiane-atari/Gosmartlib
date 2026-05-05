import { LocalDate } from '../../shared/types/local-date.type';

export type LoanExtensionType =
  | 'ONE_DAY'
  | 'ONE_WEEK'
  | 'TWO_WEEKS'
  | 'THREE_WEEKS'
  | 'CUSTOM_DATE';

export interface LoanExtensionRequest {
  extensionType: LoanExtensionType;
  customReturnDate?: LocalDate | null;
}
