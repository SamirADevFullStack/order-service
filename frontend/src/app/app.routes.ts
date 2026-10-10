import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'panier' },
  {
    path: 'panier',
    title: 'Panier',
    /* Chargement à la demande (lazy) et non au démarrage (eager) en lieu et place de component: Cart*/
    loadComponent: () => import('./features/cart/cart').then((m) => m.Cart),
  },
  {
    path: 'commandes',
    title: 'Mes commandes',
    loadComponent: () => import('./features/orders/order-list/order-list').then((m) => m.OrderList),
  },
  {
    path: 'commandes/:id',
    title: 'Détail de la commande',
    loadComponent: () =>
      import('./features/orders/order-detail/order-detail').then((m) => m.OrderDetail),
  },
  { path: '**', redirectTo: 'panier' },
];
