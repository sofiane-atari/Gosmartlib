import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../services/authorization/auth.service';
import { RoleService } from '../../services/role-service/role.service';

export const teacherOnlyGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const roleService = inject(RoleService);
  const router = inject(Router);

  if (!authService.getValidToken()) {
    return router.parseUrl('/login');
  }

  if (roleService.isTeacher() || hasTeacherRoleInToken(authService.getValidToken())) {
    return true;
  }

  return router.parseUrl('/reading-list');
};

function hasTeacherRoleInToken(token: string | null): boolean {
  if (!token) {
    return false;
  }

  const tokenParts = token.split('.');
  if (tokenParts.length !== 3) {
    return false;
  }

  try {
    const base64Payload = tokenParts[1].replace(/-/g, '+').replace(/_/g, '/');
    const paddedPayload = base64Payload.padEnd(Math.ceil(base64Payload.length / 4) * 4, '=');
    const payload = JSON.parse(atob(paddedPayload)) as { roles?: unknown };

    return payload.roles === 'ROLE_TEACHER' || payload.roles === 'TEACHER';
  } catch {
    return false;
  }
}

