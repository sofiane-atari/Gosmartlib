import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthUserDto, Role } from '../../model/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private static readonly TOKEN_STORAGE_KEY = 'authToken';
  private static readonly TOKEN_EXPIRY_STORAGE_KEY = 'authTokenExpiry';

  private readonly http = inject(HttpClient);
  private tokenExpiryTimeoutId: ReturnType<typeof setTimeout> | null = null;

  private token = signal<string | null>(
    localStorage.getItem(AuthService.TOKEN_STORAGE_KEY),
  );
  private tokenExpiryEpochMs = signal<number | null>(
    this.parseStoredExpiry(
      localStorage.getItem(AuthService.TOKEN_EXPIRY_STORAGE_KEY),
    ),
  );

  // Signal om de huidige gebruiker globaal beschikbaar te maken
  public currentUser = signal<AuthUserDto | null>(null);

  // De geselecteerde mock rol (om mee te sturen naar de backend)
  public activeMockRole = signal<Role>(
    (localStorage.getItem('mockRole') as Role) || 'STUDENT',
  );

  public isAuthenticated = computed(() => {
    const authToken = this.token();
    const expiresAt = this.tokenExpiryEpochMs();
    return !!authToken && !!expiresAt && Date.now() < expiresAt;
  });

  public constructor() {
    const authToken = this.token();
    const expiresAt = this.tokenExpiryEpochMs();

    if (!authToken || !expiresAt || Date.now() >= expiresAt) {
      this.clearToken();
      return;
    }

    this.scheduleTokenExpiry(expiresAt);
  }

  // Haal de gebruiker op van de backend
  public fetchCurrentUser(): void {
    if (!this.isAuthenticated()) {
      return;
    }

    this.http.get<AuthUserDto>(`${environment.apiUrl}/auth/me`).subscribe({
      next: (user) => this.currentUser.set(user),
      error: (err) => console.error('Fout bij ophalen gebruiker', err),
    });
  }

  // Methode om van rol te wisselen in de UI
  public switchRole(role: Role) {
    this.activeMockRole.set(role);
    localStorage.setItem('mockRole', role);
    this.fetchCurrentUser();
  }

  public hasRole(role: Role): boolean {
    return this.currentUser()?.role === role;
  }

  public storeToken(token: string): boolean {
    const expiresAt = this.extractTokenExpiry(token);

    if (!expiresAt || Date.now() >= expiresAt) {
      this.clearToken();
      return false;
    }

    localStorage.setItem(AuthService.TOKEN_STORAGE_KEY, token);
    localStorage.setItem(
      AuthService.TOKEN_EXPIRY_STORAGE_KEY,
      expiresAt.toString(),
    );
    this.token.set(token);
    this.tokenExpiryEpochMs.set(expiresAt);
    this.scheduleTokenExpiry(expiresAt);

    return true;
  }

  public getValidToken(): string | null {
    const authToken = this.token();
    const expiresAt = this.tokenExpiryEpochMs();

    if (!authToken || !expiresAt || Date.now() >= expiresAt) {
      this.clearToken();
      return null;
    }

    return authToken;
  }

  public clearToken(): void {
    this.clearExpiryTimeout();
    localStorage.removeItem(AuthService.TOKEN_STORAGE_KEY);
    localStorage.removeItem(AuthService.TOKEN_EXPIRY_STORAGE_KEY);
    this.token.set(null);
    this.tokenExpiryEpochMs.set(null);
    this.currentUser.set(null);
  }

  private scheduleTokenExpiry(expiresAt: number): void {
    this.clearExpiryTimeout();

    const timeoutMs = expiresAt - Date.now();
    if (timeoutMs <= 0) {
      this.clearToken();
      return;
    }

    this.tokenExpiryTimeoutId = setTimeout(() => {
      this.clearToken();
    }, timeoutMs);
  }

  private clearExpiryTimeout(): void {
    if (this.tokenExpiryTimeoutId) {
      clearTimeout(this.tokenExpiryTimeoutId);
      this.tokenExpiryTimeoutId = null;
    }
  }

  private parseStoredExpiry(value: string | null): number | null {
    if (!value) {
      return null;
    }

    const parsedValue = Number(value);
    if (!Number.isFinite(parsedValue) || parsedValue <= 0) {
      return null;
    }

    return parsedValue;
  }

  private extractTokenExpiry(token: string): number | null {
    const tokenParts = token.split('.');
    if (tokenParts.length !== 3) {
      return null;
    }

    try {
      const base64Payload = tokenParts[1].replace(/-/g, '+').replace(/_/g, '/');
      const paddedPayload = base64Payload.padEnd(
        Math.ceil(base64Payload.length / 4) * 4,
        '=',
      );
      const decodedPayload = atob(paddedPayload);
      const payload = JSON.parse(decodedPayload) as { exp?: unknown };

      if (typeof payload.exp !== 'number' || !Number.isFinite(payload.exp)) {
        return null;
      }

      return payload.exp * 1000;
    } catch {
      return null;
    }
  }
}
