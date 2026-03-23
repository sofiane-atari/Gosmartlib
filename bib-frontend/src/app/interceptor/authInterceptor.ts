import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('authToken');
  const expiry = localStorage.getItem('authTokenExpiry');

  if (!token || !expiry) {
    return next(req);
  }

  const isExpired = Date.now() > Number(expiry);

  if (isExpired) {
    localStorage.removeItem('authToken');
    localStorage.removeItem('authTokenExpiry');
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });

  return next(authReq);
};