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

  if (schoolOverrideService.isClassOverrideActive()) {
    const classCodes = schoolOverrideService.activeClassCodes();
    if (classCodes.length > 0) {
      headers['X-Active-Class-Codes'] = classCodes.map((c) => encodeURIComponent(c)).join(',');
    }
    const classNames = schoolOverrideService.activeClassNames();
    if (classNames.length > 0) {
      headers['X-Active-Class-Names'] = classNames.map((n) => encodeURIComponent(n)).join(',');
    }
    const primaryGroups = schoolOverrideService.activePrimaryGroupNames();
    if (primaryGroups.length > 0) {
      headers['X-Active-Primary-Group-Names'] = primaryGroups.map((g) => encodeURIComponent(g)).join(',');
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
