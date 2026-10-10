import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { Observable, catchError, map, of, switchMap } from 'rxjs';
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
  /** Le numéro de la page affichée (à partir de 0). */
  protected readonly page = signal(0);

  protected readonly view$: Observable<OrderListView> = toObservable(this.page).pipe(
    switchMap((page) =>
      this.orderService.listCommandes(page).pipe(
        map((orders): OrderListView => ({ kind: 'loaded', page: orders })),
        catchError(() =>
          of<OrderListView>({
            kind: 'error',
            message: 'Impossible de charger vos commandes. Réessayez plus tard.',
          }),
        ),
      ),
    ),
  );

  protected previous(): void {
    this.page.update((page) => page - 1);
  }

  protected next(): void {
    this.page.update((page) => page + 1);
  }
}
