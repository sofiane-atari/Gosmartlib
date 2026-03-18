export type Role = 'STUDENT' | 'TEACHER' | 'LIBRARIAN' | 'ADMIN';

export interface AuthUserDto {
  id: string;
  name: string;
  role: Role;
  email?: string | null;
  schoolName?: string | null;
  smartschoolId?: string | null;
  readingLevel?: string | null;
}