import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch admin override status when authenticated', () => {
    service.storeToken(createTokenWithExpSeconds(3600));
    service.fetchAdminOverrideStatus();

    const request = httpMock.expectOne(
      `${environment.apiUrl}/auth/admin-override/status`
    );
    expect(request.request.method).toBe('GET');

    request.flush({ enabled: true, expiresInSeconds: 600 });
    expect(service.adminOverrideStatus()).toEqual({
      enabled: true,
      expiresInSeconds: 600,
    });
  });

  it('should post admin override toggle', () => {
    service.storeToken(createTokenWithExpSeconds(3600));
    service.setAdminOverride(true);

    const request = httpMock.expectOne(`${environment.apiUrl}/auth/admin-override`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ enabled: true });

    request.flush({ enabled: true, expiresInSeconds: 1800 });

    const meRequest = httpMock.expectOne(`${environment.apiUrl}/auth/me`);
    meRequest.flush({ id: '1', role: 'ADMIN', fullName: 'Test User' });

    const statusRequest = httpMock.expectOne(
      `${environment.apiUrl}/auth/admin-override/status`
    );
    statusRequest.flush({ enabled: true, expiresInSeconds: 1800 });

    expect(service.adminOverrideStatus()?.enabled).toBeTrue();
  });
});

function createTokenWithExpSeconds(expiresInSecondsFromNow: number): string {
  const exp = Math.floor(Date.now() / 1000) + expiresInSecondsFromNow;
  const header = base64UrlEncode(JSON.stringify({ alg: 'none', typ: 'JWT' }));
  const payload = base64UrlEncode(JSON.stringify({ exp }));
  return `${header}.${payload}.signature`;
}

function base64UrlEncode(value: string): string {
  return btoa(value).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
