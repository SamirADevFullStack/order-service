import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { OrderDetail, OrderPage } from './order.model';

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private readonly http = inject(HttpClient);

  /** GET /api/orders?page=…&size=… : une page de commandes, de la plus récente à la plus ancienne. */
  listCommandes(page = 0, size = 20): Observable<OrderPage> {
    return this.http.get<OrderPage>('/api/orders', { params: { page, size } });
  }

  /** GET /api/orders/{id} : une commande et ses lignes (404 si elle n'existe pas). */
  getCommande(id: string): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`/api/orders/${id}`);
  }
}
