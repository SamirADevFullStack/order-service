import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'panier' },
  {
    path: 'panier',
    title: 'Panier',
    /* Chargement à la demande (lazy) et non au démarrage (eager) en lieu et place de component: Cart*/
    loadComponent: () => import('./features/cart/cart').then((m) => m.Cart),
  },
  { path: '**', redirectTo: 'panier' },
];
