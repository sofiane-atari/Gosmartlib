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

  return router.parseUrl('/catalog');
};
