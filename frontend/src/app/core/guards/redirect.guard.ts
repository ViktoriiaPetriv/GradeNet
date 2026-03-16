import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { map, take } from 'rxjs';

export const redirectGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const authState = inject(AuthStateService);
  const router = inject(Router);

  return authService.sessionReady$.pipe(
    take(1),
    map((isReady) => {
      if (!isReady) return router.createUrlTree(['/login']);

      if (authState.isAdminOrManager()) {
        return router.createUrlTree(['/users']);
      }
      return router.createUrlTree(['/profile', authState.currentUserId()]);
    }),
  );
};
