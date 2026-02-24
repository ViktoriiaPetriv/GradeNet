import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { ProfileComponent } from './features/users/profile/profile.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'orgs',
    canActivate: [authGuard],
    loadChildren: () => import('./features/orgs/orgs.routes').then((m) => m.orgsRoutes),
  },
  {
    path: 'specialties',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/specialties/specialties.routes').then((m) => m.specialtiesRoutes),
  },
  {
    path: 'users',
    canActivate: [authGuard],
    loadChildren: () => import('./features/users/users.routes').then((m) => m.usersRoutes),
  },
  { path: 'profile/:id', component: ProfileComponent, canActivate: [authGuard] },
  {
    path: '',
    redirectTo: 'users',
    pathMatch: 'full',
  },
];
