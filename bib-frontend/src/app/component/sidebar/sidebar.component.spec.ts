import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';

import { SidebarComponent } from './sidebar.component';
import { AuthService } from '../../services/authorization/auth.service';
import { RoleService } from '../../services/role-service/role.service';
import { provideRouter } from '@angular/router';

describe('SidebarComponent', () => {
  let component: SidebarComponent;
  let fixture: ComponentFixture<SidebarComponent>;

  const authServiceMock = {
    currentUser: signal({ id: '1', role: 'STUDENT' as const, fullName: 'Test User' }),
    adminOverrideStatus: signal({ enabled: false, expiresInSeconds: 1800 }),
    isAdminOverrideFeatureEnabled: true,
    clearToken: jasmine.createSpy('clearToken'),
    setAdminOverride: jasmine.createSpy('setAdminOverride'),
  };

  const roleServiceMock = {
    isAdmin: jasmine.createSpy('isAdmin').and.returnValue(false),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SidebarComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: RoleService, useValue: roleServiceMock },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(SidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle admin override on click handler', () => {
    component.toggleAdminOverride();
    expect(authServiceMock.setAdminOverride).toHaveBeenCalledWith(true);
  });
});
