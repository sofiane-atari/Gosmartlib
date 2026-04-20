import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthUserDto } from '../../model/auth.model';
import { environment } from '../../../environments/environment';

export interface AdminOverrideStatusDto {
  enabled: boolean;
  expiresInSeconds: number | null;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private static readonly TOKEN_STORAGE_KEY = 'authToken';
  private static readonly TOKEN_EXPIRY_STORAGE_KEY = 'authTokenExpiry';
  private static readonly FALLBACK_TOKEN_LIFETIME_MS = 60 * 60 * 1000;

  private readonly http = inject(HttpClient);
  private tokenExpiryTimeoutId: ReturnType<typeof setTimeout> | null = null;

  public readonly currentUser = signal<AuthUserDto | null>(null);
  public readonly isLoading = signal(false);
  public readonly adminOverrideStatus = signal<AdminOverrideStatusDto | null>(null);
  public readonly isAdminOverrideLoading = signal(false);
  public readonly isAdminOverrideFeatureEnabled = environment.adminOverrideEnabled;

  private token = signal<string | null>(
    localStorage.getItem(AuthService.TOKEN_STORAGE_KEY),
  );

  private tokenExpiryEpochMs = signal<number | null>(
    this.parseStoredExpiry(
      localStorage.getItem(AuthService.TOKEN_EXPIRY_STORAGE_KEY),
    ),
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
      this.currentUser.set(null);
      this.adminOverrideStatus.set(null);
      return;
    }

    this.isLoading.set(true);

    this.http.get<AuthUserDto>(`${environment.apiUrl}/auth/me`).subscribe({
      next: (user) => {
        this.currentUser.set(user);
        this.isLoading.set(false);
        this.fetchAdminOverrideStatus();
      },
      error: (err) => {
        console.error('Fout bij ophalen gebruiker', err);
        this.currentUser.set(null);
        this.adminOverrideStatus.set(null);
        this.isLoading.set(false);
      },
    });
  }

  public fetchAdminOverrideStatus(): void {
    if (!this.isAdminOverrideFeatureEnabled || !this.isAuthenticated()) {
      this.adminOverrideStatus.set(null);
      return;
    }

    this.isAdminOverrideLoading.set(true);
    this.http
      .get<AdminOverrideStatusDto>(`${environment.apiUrl}/auth/admin-override/status`)
      .subscribe({
        next: (status) => {
          this.adminOverrideStatus.set(status);
          this.isAdminOverrideLoading.set(false);
        },
        error: () => {
          this.adminOverrideStatus.set(null);
          this.isAdminOverrideLoading.set(false);
        },
      });
  }

  public setAdminOverride(enabled: boolean): void {
    if (!this.isAdminOverrideFeatureEnabled || !this.isAuthenticated()) {
      return;
    }

    this.isAdminOverrideLoading.set(true);
    this.http
      .post<AdminOverrideStatusDto>(`${environment.apiUrl}/auth/admin-override`, {
        enabled,
      })
      .subscribe({
        next: (status) => {
          this.adminOverrideStatus.set(status);
          this.isAdminOverrideLoading.set(false);
          this.fetchCurrentUser();
        },
        error: (err) => {
          console.error('Fout bij aanpassen admin override', err);
          this.isAdminOverrideLoading.set(false);
        },
      });
  }

  public storeToken(token: string): boolean {
    const normalizedToken = this.normalizeToken(token);
    if (!normalizedToken) {
      this.clearToken();
      return false;
    }

    const expiresAt =
      this.extractTokenExpiry(normalizedToken) ??
      Date.now() + AuthService.FALLBACK_TOKEN_LIFETIME_MS;

    if (Date.now() >= expiresAt) {
      this.clearToken();
      return false;
    }

    localStorage.setItem(AuthService.TOKEN_STORAGE_KEY, normalizedToken);
    localStorage.setItem(
      AuthService.TOKEN_EXPIRY_STORAGE_KEY,
      expiresAt.toString(),
    );
    this.token.set(normalizedToken);
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
    this.adminOverrideStatus.set(null);
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

  private normalizeToken(token: string): string | null {
    const trimmedToken = token.trim();
    if (!trimmedToken) {
      return null;
    }

    if (trimmedToken.toLowerCase().startsWith('bearer ')) {
      const bearerToken = trimmedToken.slice(7).trim();
      return bearerToken || null;
    }

    return trimmedToken;
  }
}
