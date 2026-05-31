import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../services/authorization/auth.service';
import { RoleService } from '../../services/role-service/role.service';

export const librarianOrAdminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const roleService = inject(RoleService);
  const router = inject(Router);

  const token = authService.getValidToken();

  if (!token) {
    return router.parseUrl('/login');
  }

    if (roleService.isLibrarian() || roleService.isAdmin()) {
        return true;
    }

    if (hasLibrarianOrAdminRoleInToken(token)) {
        return true;
    }

    return router.parseUrl('/catalog');
};

function hasLibrarianOrAdminRoleInToken(token: string): boolean {
    const tokenParts = token.split('.');
    if (tokenParts.length !== 3) {
        return false;
    }
    try {
        const base64Payload = tokenParts[1].replace(/-/g, '+').replace(/_/g, '/');
        const paddedPayload = base64Payload.padEnd(Math.ceil(base64Payload.length / 4) * 4, '=');
        const payload = JSON.parse(atob(paddedPayload)) as { roles?: unknown };

        if (typeof payload.roles === 'string') {
            return payload.roles === 'ROLE_LIBRARIAN' || payload.roles === 'LIBRARIAN'
                || payload.roles === 'ROLE_ADMIN' || payload.roles === 'ADMIN';
        }

        return false;
    } catch {
        return false;
    }
}
