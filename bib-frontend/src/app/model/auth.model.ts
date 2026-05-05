export type Role = 'STUDENT' | 'TEACHER' | 'LIBRARIAN' | 'ADMIN';

export interface SchoolSummaryDto {
  id: number;
  name: string;
  defaultLoanDurationDays?: number;
}

export interface AuthUserDto {
  id: string;
  firstName?: string | null;
  lastName?: string | null;
  fullName?: string | null;
  role: Role;
  email?: string | null;
  smartschoolId?: string | null;
  platform?: string | null;
  primaryGroupName?: string | null;
  parentGroupNames?: string[] | null;
  reviewBlocked?: boolean;
  school?: SchoolSummaryDto;
}
