import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/RoleAuthorization/auth.service';

export const mockAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const currentMockRole = authService.activeMockRole();
  const authToken = authService.getValidToken();

  // Kloon het request en voeg de header toe
  const headers: Record<string, string> = {
    'X-Mock-Role': currentMockRole,
  };

  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`;
  }

  const modifiedReq = req.clone({
    setHeaders: headers,
  });

  return next(modifiedReq);
};
