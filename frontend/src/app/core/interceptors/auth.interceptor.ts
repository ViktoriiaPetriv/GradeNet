import { HttpInterceptorFn, HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { TokenService } from '../services/token.service';
import { AuthService } from '../services/auth.service';

const addToken = (req: HttpRequest<unknown>, token: string) =>
  req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const token = tokenService.getToken();

  const authReq = token ? addToken(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/auth/')) {
        return authService.refresh().pipe(
          switchMap((res) => next(addToken(req, res.token))),
          catchError(() => {
            authService.logout();
            return throwError(() => error);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
