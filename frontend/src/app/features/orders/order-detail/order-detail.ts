import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, catchError, map, of, switchMap } from 'rxjs';
import { OrderService } from '../order-service';
import { OrderDetail as Order } from '../order.model';

/** Ce que la page peut afficher : la commande, « introuvable » (404), ou une autre erreur. */
type OrderDetailView =
  { kind: 'loaded'; order: Order } | { kind: 'not-found' } | { kind: 'error'; message: string };

@Component({
  selector: 'app-order-detail',
  imports: [AsyncPipe, CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss',
})
export class OrderDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);

  protected readonly view$: Observable<OrderDetailView> = this.route.paramMap.pipe(
    map((params) => params.get('id') ?? ''),
    switchMap((id) =>
      this.orderService.getCommande(id).pipe(
        map((order): OrderDetailView => ({ kind: 'loaded', order })),
        catchError((error: HttpErrorResponse) =>
          of<OrderDetailView>(
            error.status === 404
              ? { kind: 'not-found' }
              : {
                  kind: 'error',
                  message: 'Impossible de charger la commande. Réessayez plus tard.',
                },
          ),
        ),
      ),
    ),
  );
}
