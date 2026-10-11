import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DEFAULT_ORDER_SORT, OrderDetail, OrderPage, OrderSort } from './order.model';

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private readonly http = inject(HttpClient);

  /**
   * GET /api/orders?page=…&size=…[&q=…][&sort=…] : une page de commandes.
   * q et sort ne sont envoyés que s'ils sont utiles : texte non vide, tri différent de celui par défaut.
   */
  listCommandes(
    page = 0,
    size = 20,
    q = '',
    sort: OrderSort = DEFAULT_ORDER_SORT,
  ): Observable<OrderPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q.trim() !== '') {
      params = params.set('q', q.trim());
    }
    if (sort !== DEFAULT_ORDER_SORT) {
      params = params.set('sort', sort);
    }
    return this.http.get<OrderPage>('/api/orders', { params });
  }

  /** GET /api/orders/{id} : une commande et ses lignes (404 si elle n'existe pas). */
  getCommande(id: string): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`/api/orders/${id}`);
  }
}
