import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthUserDto, Role } from '../../model/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private url = `${environment.apiUrl}/auth/me`

  public readonly currentUser = signal<AuthUserDto | null>(null);
  public readonly isLoading = signal(false);

  public readonly activeMockRole = signal<Role>(
    (localStorage.getItem('mockRole') as Role) || 'STUDENT'
  );

  public fetchCurrentUser(onError?: () => void): void {
    this.isLoading.set(true);

    this.http.get<AuthUserDto>(this.url).subscribe({
      next: (user: AuthUserDto) => {
        this.currentUser.set(user);
        this.isLoading.set(false);
      },
      error: () => {
        this.currentUser.set(null);
        this.isLoading.set(false);
        onError?.();
      }
    });
  }

  public switchRole(role: Role, onError?: () => void): void {
    this.activeMockRole.set(role);
    localStorage.setItem('mockRole', role);
    this.fetchCurrentUser(onError);
  }

  public hasRole(role: Role): boolean {
    return this.currentUser()?.role === role;
  }
}