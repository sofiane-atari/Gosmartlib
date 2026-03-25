export type Role = 'STUDENT' | 'TEACHER' | 'LIBRARIAN' | 'ADMIN';

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
}
