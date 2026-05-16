import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthUserDto } from '../../model/auth.model';

export interface AssignLibrarianRequest {
  userId: string;
}

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  public assignLibrarian(userId: string): Observable<AuthUserDto> {
    return this.http.post<AuthUserDto>(`${this.apiUrl}/roles/assign-librarian`, { userId });
  }

  public revokeLibrarian(userId: string): Observable<AuthUserDto> {
    return this.http.post<AuthUserDto>(`${this.apiUrl}/roles/revoke-librarian`, { userId });
  }
}
