import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/authorization/auth.service';
import { SchoolOverrideService } from '../services/school-override/school-override.service';

const REDIRECT_PATHS = ['/login', '/unauthorized'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const injector = inject(Injector);
  const schoolOverrideService = inject(SchoolOverrideService);

  const token = localStorage.getItem('authToken');
  const expiry = localStorage.getItem('authTokenExpiry');

  if (!token || !expiry) {
    return next(req);
  }

  const isExpired = Date.now() > Number(expiry);

  if (isExpired) {
    injector.get(AuthService).clearToken();
    return next(req);
  }

  const headers: Record<string, string> = {
    Authorization: `Bearer ${token}`,
  };

  if (schoolOverrideService.isActive()) {
    const activeId = schoolOverrideService.activeSchoolId();
    if (activeId !== null && activeId !== undefined) {
      headers['X-Active-School-Id'] = activeId.toString();
    }
  }

  const authReq = req.clone({
    setHeaders: headers,
  });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (REDIRECT_PATHS.includes(router.url)) {
        return throwError(() => error);
      }

      const code = error.error?.code;

      if (error.status === 401 && code === 'TOKEN_INVALID') {
        injector.get(AuthService).logout();
      }

      if (error.status === 403 && code === 'FORBIDDEN') {
        router.navigate(['/unauthorized']);
      }

      return throwError(() => error);
    }),
  );
};
