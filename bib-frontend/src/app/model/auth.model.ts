export type Role = 'STUDENT' | 'TEACHER' | 'LIBRARIAN' | 'ADMIN';

export interface SchoolSummaryDto {
  id: number;
  name: string;
  domainName: string;
  defaultLoanDurationDays?: number;
  status?: string;
}

export interface AuthUserDto {
  id: string;
  firstName?: string | null;
  lastName?: string | null;
  fullName?: string | null;
  role: Role;
  email?: string | null;
  username?: string | null;
  smartschoolId?: string | null;
  platform?: string | null;
  primaryGroupName?: string | null;
  parentGroupNames?: string[] | null;
  reviewBlocked?: boolean;
  schoolId?: number | null;
  schoolName?: string | null;
  school?: SchoolSummaryDto;
}
