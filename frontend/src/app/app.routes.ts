import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { redirectGuard } from './core/guards/redirect.guard';
import { setupCompleteGuard, setupRequiredGuard } from './core/guards/setup.guard';

export const routes: Routes = [
  {
    path: 'setup',
    canActivate: [setupRequiredGuard],
    loadComponent: () =>
      import('./features/auth/setup/setup.component').then((m) => m.SetupComponent),
  },
  {
    path: 'login',
    canActivate: [setupCompleteGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'orgs',
    canActivate: [authGuard(['ADMIN'])],
    loadComponent: () =>
      import('./features/orgs/org-list/org-list.component').then((m) => m.OrgListComponent),
  },
  {
    path: 'orgs/:id',
    canActivate: [authGuard(['ADMIN'])],
    loadComponent: () =>
      import('./features/orgs/org-detail/org-detail.component').then((m) => m.OrgDetailComponent),
  },
  {
    path: 'specialties',
    canActivate: [authGuard(['ADMIN', 'MANAGER'])],
    loadChildren: () =>
      import('./features/specialties/specialties.routes').then((m) => m.specialtiesRoutes),
  },
  {
    path: 'specialties/:id',
    canActivate: [authGuard(['ADMIN', 'MANAGER'])],
    loadComponent: () =>
      import('./features/specialties/specialty-detail/specialty-detail.component').then(
        (m) => m.SpecialtyDetailComponent,
      ),
  },
  {
    path: 'users',
    canActivate: [authGuard(['ADMIN', 'MANAGER'])],
    loadChildren: () => import('./features/users/users.routes').then((m) => m.usersRoutes),
  },
  {
    path: 'profile/:id',
    canActivate: [authGuard()],
    loadComponent: () =>
      import('./features/users/profile/profile.component').then((m) => m.ProfileComponent),
  },
  {
    path: 'disciplines',
    canActivate: [authGuard(['ADMIN', 'MANAGER'])],
    loadComponent: () =>
      import('./features/disciplines/discipline-list/discipline-list.component').then(
        (m) => m.DisciplineListComponent,
      ),
  },
  {
    path: 'disciplines/:id',
    canActivate: [authGuard(['ADMIN', 'MANAGER'])],
    loadComponent: () =>
      import('./features/disciplines/discipline-detail/discipline-detail.component').then(
        (m) => m.DisciplineDetailComponent,
      ),
  },
  {
    path: 'groups',
    canActivate: [authGuard(['ADMIN', 'MANAGER'])],
    loadComponent: () =>
      import('./features/groups/group-list/group-list.component').then((m) => m.GroupListComponent),
  },
  {
    path: 'groups/:id',
    canActivate: [authGuard(['ADMIN', 'MANAGER'])],
    loadComponent: () =>
      import('./features/groups/group-detail/group-detail.component').then((m) => m.GroupDetailComponent),
  },
  {
    path: 'books',
    canActivate: [authGuard(['ADMIN', 'MANAGER'])],
    loadComponent: () =>
      import('./features/books/book-list/book-list.component').then((m) => m.BookListComponent),
  },
  {
    path: 'books/:id',
    canActivate: [authGuard(['ADMIN', 'MANAGER'])],
    loadComponent: () =>
      import('./features/books/book-detail/book-detail.component').then(
        (m) => m.BookDetailComponent,
      ),
  },
  {
    path: 'grades',
    canActivate: [authGuard(['ADMIN', 'PROFESSOR'])],
    loadComponent: () =>
      import('./features/grades/entry-list/entry-list.component').then((m) => m.EntryListComponent),
  },
  {
    path: 'grades/student/:bookNumberId/report',
    canActivate: [authGuard(['ADMIN', 'MANAGER', 'PROFESSOR', 'STUDENT'])],
    loadComponent: () =>
      import('./features/grades/student-grade-report/student-grade-report.component').then(
        (m) => m.StudentGradeReportComponent,
      ),
  },
  {
    path: 'grades/student/:bookNumberId',
    canActivate: [authGuard(['ADMIN', 'MANAGER', 'PROFESSOR', 'STUDENT'])],
    loadComponent: () =>
      import('./features/grades/student-grades/student-grades.component').then(
        (m) => m.StudentGradesComponent,
      ),
  },
  {
    path: 'grades/bulk',
    canActivate: [authGuard(['ADMIN', 'PROFESSOR'])],
    loadComponent: () =>
      import('./features/grades/bulk-grade/bulk-grade.component').then((m) => m.BulkGradeComponent),
  },
  {
    path: 'grades/group-report',
    canActivate: [authGuard(['ADMIN', 'PROFESSOR'])],
    loadComponent: () =>
      import('./features/grades/group-report/group-report.component').then(
        (m) => m.GroupReportComponent,
      ),
  },
  {
    path: 'grades/:id',
    canActivate: [authGuard(['ADMIN', 'PROFESSOR'])],
    loadComponent: () =>
      import('./features/grades/entry-detail/entry-detail.component').then(
        (m) => m.EntryDetailComponent,
      ),
  },
  {
    path: 'import',
    canActivate: [authGuard(['ADMIN', 'MANAGER'])],
    loadComponent: () =>
      import('./features/import/import.component').then((m) => m.ImportComponent),
  },
  {
    path: 'journal',
    canActivate: [authGuard(['ADMIN', 'MANAGER', 'PROFESSOR'])],
    loadComponent: () =>
      import('./features/journal/journal.component').then((m) => m.JournalComponent),
  },
  {
    path: 'additional-works',
    canActivate: [authGuard(['ADMIN', 'MANAGER', 'PROFESSOR'])],
    loadComponent: () =>
      import('./features/additional-works/additional-work-list/additional-work-list.component').then(
        (m) => m.AdditionalWorkListComponent,
      ),
  },
  {
    path: 'additional-works/:id',
    canActivate: [authGuard(['ADMIN', 'MANAGER', 'PROFESSOR'])],
    loadComponent: () =>
      import('./features/additional-works/additional-work-detail/additional-work-detail.component').then(
        (m) => m.AdditionalWorkDetailComponent,
      ),
  },
  {
    path: 'commissions',
    canActivate: [authGuard(['ADMIN', 'MANAGER', 'PROFESSOR'])],
    loadComponent: () =>
      import('./features/commissions/commission-list/commission-list.component').then(
        (m) => m.CommissionListComponent,
      ),
  },
  {
    path: 'commissions/:id',
    canActivate: [authGuard(['ADMIN', 'MANAGER', 'PROFESSOR'])],
    loadComponent: () =>
      import('./features/commissions/commission-detail/commission-detail.component').then(
        (m) => m.CommissionDetailComponent,
      ),
  },
  {
    path: '',
    canActivate: [setupCompleteGuard, redirectGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
];
