import { Routes } from '@angular/router';

export const orgsRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./org-list/org-list.component').then((m) => m.OrgListComponent),
  },
];
