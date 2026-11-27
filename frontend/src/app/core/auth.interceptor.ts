import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const authed = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(authed).pipe(
    catchError((e: HttpErrorResponse) => {
      // an expired token on a protected call sends the user back to the login page
      if (e.status === 401 && token && !req.url.includes('/api/auth/')) auth.logout();
      return throwError(() => e);
    })
  );
};

export function errorMessage(e: any, fallback = 'Something went wrong'): string {
  return e?.error?.message ?? (e?.status === 0 ? 'Cannot reach the server. Is the backend running on port 8080?' : fallback);
}
