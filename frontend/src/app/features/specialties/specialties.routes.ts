import { Routes } from '@angular/router';

export const specialtiesRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./specialty-list/specialty-list.component').then((m) => m.SpecialtyListComponent),
  },
];
