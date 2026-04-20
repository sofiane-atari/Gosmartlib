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

  if (roleService.canManageDidacticMaterials()) {
    return true;
  }

  return router.parseUrl('/catalog');
};
