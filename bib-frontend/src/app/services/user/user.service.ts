import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthUserDto, Role } from '../../model/auth.model';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model';
import { TeacherStats } from '../../model/user/teacher-stats.model';

export interface GetUsersOpt {
  WithfirstName?: string;
  WithlastName?: string;
  WithId?: String;
  withGroups?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  public getTeacherStats(): Observable<TeacherStats> {
    return this.http.get<TeacherStats>(`${this.apiUrl}/users/teachers/stats`);
  }

  public getTeachers(
    excludeAdmins: boolean = true,
    page: number = 0,
    pageSize: number = 20,
    search?: string,
  ): Observable<PaginatedResponse<AuthUserDto>> {
    let params = new HttpParams()
      .set('excludeAdmins', excludeAdmins)
      .set('page', page)
      .set('pageSize', pageSize);

    if (search) {
      params = params.set('search', search);
    }

    return this.http.get<PaginatedResponse<AuthUserDto>>(`${this.apiUrl}/users/teachers`, { params });
  }

  public getUsers(opt?: GetUsersOpt): Observable<AuthUserDto[]> {
    let params = new HttpParams({
      fromObject: {
        firstName: String(opt?.WithfirstName ?? ''),
        lastName: String(opt?.WithlastName ?? ''),
        id: String(opt?.WithId ?? ''),
        withGroups: String(opt?.withGroups ?? false),
      },
    });

    return this.http.get<AuthUserDto[]>(`${this.apiUrl}/users`, {
      params: params,
    });
  }

  public searchExternalUsers(
    schoolId: number,
    firstName: string = '',
    lastName: string = '',
  ): Observable<AuthUserDto[]> {
    const params = new HttpParams()
      .set('schoolId', schoolId)
      .set('firstName', firstName)
      .set('lastName', lastName);

    return this.http.get<AuthUserDto[]>(`${this.apiUrl}/users/search-external`, { params });
  }


  public getDisplayName(user: AuthUserDto | null): string {
    if (!user) {
      return 'Gebruiker';
    }

    return (
      `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() ||
      user.fullName?.trim() ||
      user.firstName?.trim() ||
      user.username?.trim() ||
      user.id ||
      'Gebruiker'
    );
  }

  public getRoleLabel(role: Role | null | undefined): string {
    switch (role) {
      case 'STUDENT':
        return 'Leerling';
      case 'TEACHER':
        return 'Leerkracht';
      case 'LIBRARIAN':
        return 'Beheerder';
      case 'ADMIN':
        return 'Admin';
      default:
        return 'Gebruiker';
    }
  }

  public isOwnResource(
    currentUserId: string | null | undefined,
    ownerUserId: string | null | undefined,
  ): boolean {
    return !!currentUserId && !!ownerUserId && currentUserId === ownerUserId;
  }


  public updateReviewAccess(userId: string, blocked: boolean): Observable<AuthUserDto> {
    return this.http.patch<AuthUserDto>(`${this.apiUrl}/users/${userId}/review-access`, { blocked });
  }

  public getUsersBySchoolId(schoolId: number): Observable<AuthUserDto[]> {
    return this.http.get<AuthUserDto[]>(`${this.apiUrl}/users/by-school/${schoolId}`);
  }
}
