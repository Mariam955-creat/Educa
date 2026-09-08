import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/** Endpoints qui ne portent jamais de jeton et ne déclenchent pas de refresh. */
const AUTH_FREE = ['/auth/login', '/auth/register', '/auth/refresh'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  const isAuthFree = AUTH_FREE.some((p) => req.url.includes(p));
  const token = auth.accessToken;
  const authReq =
    token && !isAuthFree ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !isAuthFree && auth.refreshToken) {
        return auth.refresh().pipe(
          switchMap((res) =>
            next(authReq.clone({ setHeaders: { Authorization: `Bearer ${res.accessToken}` } })),
          ),
          catchError((refreshErr) => {
            auth.logout();
            return throwError(() => refreshErr);
          }),
        );
      }
      return throwError(() => err);
    }),
  );
};
