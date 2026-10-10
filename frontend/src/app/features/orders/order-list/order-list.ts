import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { OrderService } from '../order-service';
import { OrderPage } from '../order.model';

/** Ce que la page peut afficher : la liste chargée, ou une erreur. */
type OrderListView = { kind: 'loaded'; page: OrderPage } | { kind: 'error'; message: string };

@Component({
  selector: 'app-order-list',
  imports: [AsyncPipe, CurrencyPipe, DatePipe],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss',
})
export class OrderList {
  private readonly orderService = inject(OrderService);

  protected readonly view$: Observable<OrderListView> = this.orderService.listCommandes().pipe(
    map((page): OrderListView => ({ kind: 'loaded', page })),
    catchError(() =>
      of<OrderListView>({
        kind: 'error',
        message: 'Impossible de charger vos commandes. Réessayez plus tard.',
      }),
    ),
  );
}
