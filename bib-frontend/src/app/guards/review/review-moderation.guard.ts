import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RoleService } from '../../services/role-service/role.service';

export const reviewModerationGuard: CanActivateFn = () => {
  const roleService = inject(RoleService);
  const router = inject(Router);

  if (roleService.canModerateReviews()) {
    return true;
  }

  return router.createUrlTree(['/catalog']);
};