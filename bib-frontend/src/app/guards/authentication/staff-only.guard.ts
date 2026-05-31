import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../services/authorization/auth.service';
import { RoleService } from '../../services/role-service/role.service';

export const staffOnlyGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const roleService = inject(RoleService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.parseUrl('/login');
  }

    if (roleService.canManageLessonTip()) {
        return true;
    }

    const token = authService.getValidToken();
    if (token && hasStaffRoleInToken(token)) {
        return true;
    }

    return router.parseUrl('/catalog');
};

function hasStaffRoleInToken(token: string | null): boolean {
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

        if (typeof payload.roles === 'string') {
            return ['ROLE_TEACHER', 'TEACHER', 'ROLE_ADMIN', 'ADMIN', 'ROLE_LIBRARIAN', 'LIBRARIAN'].includes(payload.roles);
        }

        return false;
    } catch {
        return false;
    }
}
