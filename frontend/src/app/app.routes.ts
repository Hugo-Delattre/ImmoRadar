import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/deal-finder/deal-finder.component').then(m => m.DealFinderComponent)
  }
];
