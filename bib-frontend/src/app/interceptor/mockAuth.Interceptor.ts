import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/RoleAuthorization/auth.service';


export const mockAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const currentMockRole = authService.activeMockRole();

  // Kloon het request en voeg de header toe
  const modifiedReq = req.clone({
    setHeaders: {
      'X-Mock-Role': currentMockRole
    }
  });

  return next(modifiedReq);
};