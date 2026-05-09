import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { SetupService } from '../services/setup.service';

export const setupRequiredGuard: CanActivateFn = () => {
  const setupService = inject(SetupService);
  const router = inject(Router);

  return setupService.checkSetupRequired().pipe(
    map((required) => (required ? true : router.createUrlTree(['/login']))),
  );
};

export const setupCompleteGuard: CanActivateFn = () => {
  const setupService = inject(SetupService);
  const router = inject(Router);

  return setupService.checkSetupRequired().pipe(
    map((required) => (required ? router.createUrlTree(['/setup']) : true)),
  );
};
