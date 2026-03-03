export type Role = 'STUDENT' | 'TEACHER' | 'LIBRARIAN' | 'ADMIN';

export interface AuthUserDto {
  id: string;
  name: string;
  role: Role;
}