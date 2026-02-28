import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'orgs',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orgs/org-list/org-list.component').then((m) => m.OrgListComponent),
  },
  {
    path: 'orgs/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orgs/org-detail/org-detail.component').then((m) => m.OrgDetailComponent),
  },
  {
    path: 'specialties',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/specialties/specialties.routes').then((m) => m.specialtiesRoutes),
  },
  {
    path: 'specialties/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/specialties/specialty-detail/specialty-detail.component').then(
        (m) => m.SpecialtyDetailComponent,
      ),
  },
  {
    path: 'users',
    canActivate: [authGuard],
    loadChildren: () => import('./features/users/users.routes').then((m) => m.usersRoutes),
  },
  {
    path: 'profile/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/users/profile/profile.component').then((m) => m.ProfileComponent),
  },
  {
    path: 'books',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/books/book-list/book-list.component').then((m) => m.BookListComponent),
  },
  {
    path: 'books/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/books/book-detail/book-detail.component').then(
        (m) => m.BookDetailComponent,
      ),
  },
  {
    path: '',
    redirectTo: 'users',
    pathMatch: 'full',
  },
];
