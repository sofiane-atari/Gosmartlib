import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthUserDto } from '../../model/auth.model';

export interface AssignLibrarianRequest {
  userId: string;
  schoolCode?: string;
}

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  public assignLibrarian(userId: string, schoolCode?: string): Observable<AuthUserDto> {
    return this.http.post<AuthUserDto>(`${this.apiUrl}/roles/assign-librarian`, { userId, schoolCode });
  }

  public revokeLibrarian(userId: string, schoolCode?: string): Observable<AuthUserDto> {
    return this.http.post<AuthUserDto>(`${this.apiUrl}/roles/revoke-librarian`, { userId, schoolCode });
  }

  public assignAdmin(userId: string, schoolCode?: string): Observable<AuthUserDto> {
    return this.http.post<AuthUserDto>(`${this.apiUrl}/roles/assign-admin`, { userId, schoolCode });
  }

  public revokeAdmin(userId: string, schoolCode?: string): Observable<AuthUserDto> {
    return this.http.post<AuthUserDto>(`${this.apiUrl}/roles/revoke-admin`, { userId, schoolCode });
  }

  public isAdminPromotionEnabled(): Observable<{ enabled: boolean }> {
    return this.http.get<{ enabled: boolean }>(`${this.apiUrl}/roles/admin-promotion-enabled`)
      .pipe(shareReplay(1));
  }
}
