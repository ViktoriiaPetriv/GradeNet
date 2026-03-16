import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { ToastService } from '../services/toast.service';
import { map, take } from 'rxjs';

type Role = 'ADMIN' | 'MANAGER' | 'PROFESSOR' | 'STUDENT';

export const authGuard =
  (allowedRoles?: Role[]): CanActivateFn =>
  () => {
    const authService = inject(AuthService);
    const authState = inject(AuthStateService);
    const router = inject(Router);
    const toast = inject(ToastService);

    return authService.sessionReady$.pipe(
      take(1),
      map((isReady) => {
        if (!isReady) return router.createUrlTree(['/login']);

        if (allowedRoles && !allowedRoles.includes(authState.role() as Role)) {
          toast.error('У вас немає прав для перегляду цієї сторінки');
          return false;
        }

        return true;
      }),
    );
  };
