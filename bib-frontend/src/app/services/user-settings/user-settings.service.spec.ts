import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { UserSettingsService } from './user-settings.service';
import { AuthService } from '../authorization/auth.service';
import { signal } from '@angular/core';
import { AuthUserDto } from '../../model/auth.model';

describe('UserSettingsService', () => {
  let service: UserSettingsService;
  let authServiceMock: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj<AuthService>('AuthService', [], {
      currentUser: signal<AuthUserDto | null>({ id: '1', role: 'ADMIN', schoolId: 1, schoolName: 'Test School' }),
    });

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authSpy },
      ],
    });
    service = TestBed.inject(UserSettingsService);
    authServiceMock = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
