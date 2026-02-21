import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { TokenService } from '../services/token.service';
import { map, take } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (authService.getSessionState() !== null) {
    return tokenService.isAuthenticated() ? true : router.createUrlTree(['/login']);
  }

  return authService.sessionReady$.pipe(
    take(1),
    map((ready) => (ready ? true : router.createUrlTree(['/login']))),
  );
};
