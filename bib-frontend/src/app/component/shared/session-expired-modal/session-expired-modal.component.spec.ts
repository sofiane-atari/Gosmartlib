import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';

import { AuthService } from '../../../services/authorization/auth.service';
import { LoginService } from '../../../services/login/login.service';
import { SessionExpiryModalComponent } from './session-expired-modal.component';

describe('SessionExpiryModalComponent', () => {
  let component: SessionExpiryModalComponent;
  let fixture: ComponentFixture<SessionExpiryModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SessionExpiryModalComponent],
      providers: [
        {
          provide: AuthService,
          useValue: {
            isSessionWarningVisible: signal(false),
            sessionSecondsRemaining: signal(0),
            isSessionRefreshLoading: signal(false),
            refreshSessionFromWarning: jasmine.createSpy('refreshSessionFromWarning'),
            dismissSessionWarningAndLogout: jasmine.createSpy('dismissSessionWarningAndLogout'),
          },
        },
        { provide: LoginService, useValue: {} },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(SessionExpiryModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
