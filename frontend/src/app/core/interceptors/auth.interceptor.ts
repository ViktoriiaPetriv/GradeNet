import { HttpInterceptorFn, HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take } from 'rxjs';
import { TokenService } from '../services/token.service';
import { AuthApiService } from '../services/auth-api.service';
import { Router } from '@angular/router';
import { ToastService } from '../services/toast.service';
import { LoaderService } from '../services/loader.service';
import { finalize } from 'rxjs';

let isRefreshing = false;
const refreshDone$ = new BehaviorSubject<string | null>(null);

const addToken = (req: HttpRequest<unknown>, token: string) =>
  req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const authApi = inject(AuthApiService);
  const router = inject(Router);
  const toastService = inject(ToastService);
  const loaderService = inject(LoaderService);
  const token = tokenService.getToken();

  const authReq = token && !req.url.includes('/api/auth/') ? addToken(req, token) : req;
  const skipLoader = req.url.includes('/api/auth/refresh');

  if (!skipLoader) loaderService.show();

  return next(authReq).pipe(
    finalize(() => {
      if (!skipLoader) loaderService.hide();
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/auth/')) {
        if (isRefreshing) {
          return refreshDone$.pipe(
            filter((t): t is string => t !== null),
            take(1),
            switchMap((newToken) => next(addToken(req, newToken))),
          );
        }

        isRefreshing = true;
        refreshDone$.next(null);

        return authApi.refresh().pipe(
          switchMap((res) => {
            isRefreshing = false;
            tokenService.setToken(res.token);
            tokenService.currentUser.set(res.user);
            tokenService.isAuthenticated.set(true);
            refreshDone$.next(res.token);
            return next(addToken(req, res.token));
          }),
          catchError((err) => {
            isRefreshing = false;
            tokenService.clear();
            toastService.error('Сесія закінчилась. Увійдіть знову.');
            router.navigate(['/login']);
            return throwError(() => err);
          }),
        );
      }

      if (error.status !== 401) {
        toastService.error(extractErrorMessage(error));
      }

      return throwError(() => error);
    }),
  );
};


function extractErrorMessage(error: HttpErrorResponse): string {
  if (error.error?.message) return error.error.message;
  if (error.error?.errors?.length) {
    return error.error.errors.map((e: { message: string }) => e.message).join('\n');
  }
  switch (error.status) {
    case 400:
      return 'Некоректний запит';
    case 403:
      return 'Доступ заборонено';
    case 404:
      return 'Ресурс не знайдено';
    case 500:
      return 'Помилка сервера';
    default:
      return 'Щось пішло не так';
  }
}
