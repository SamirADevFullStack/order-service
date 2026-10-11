import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import {
  Observable,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  switchMap,
} from 'rxjs';
import { OrderService } from '../order-service';
import { DEFAULT_ORDER_SORT, OrderPage, OrderSort } from '../order.model';

/** Ce que la page peut afficher : la liste chargée, ou une erreur. */
type OrderListView = { kind: 'loaded'; page: OrderPage } | { kind: 'error'; message: string };

/** Délai sans frappe avant de lancer la recherche. */
const SEARCH_DEBOUNCE_MS = 300;

@Component({
  selector: 'app-order-list',
  imports: [AsyncPipe, CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss',
})
export class OrderList {
  private readonly orderService = inject(OrderService);

  /** Le texte tapé dans le champ, à chaque frappe. */
  protected readonly searchText = signal('');

  /**
   * Le texte réellement recherché : 300 ms après la dernière frappe, sans espaces autour,
   * et seulement s'il a changé (taper puis effacer une lettre ne relance pas la recherche).
   * Remarque : comme le résultat arrive dans un signal, qui ignore déjà une valeur identique,
   * distinctUntilChanged fait ici double emploi. Il reste indispensable si l'on consomme le flux
   * comme un Observable (sans signal), par exemple directement dans un switchMap.
   */
  protected readonly query = toSignal(
    toObservable(this.searchText).pipe(
      debounceTime(SEARCH_DEBOUNCE_MS),
      map((text) => text.trim()),
      distinctUntilChanged(),
    ),
    { initialValue: '' },
  );

  /** L'ordre de la liste. */
  protected readonly sort = signal<OrderSort>(DEFAULT_ORDER_SORT);

  /**
   * Le numéro de la page affichée (à partir de 0). Il revient à 0 quand la recherche ou le tri change :
   * sinon, le client resterait sur la page 3 d'un résultat qui n'en a plus qu'une.
   */
  protected readonly page = linkedSignal({
    source: () => ({ query: this.query(), sort: this.sort() }),
    computation: () => 0,
  });

  /** Tout ce qui détermine la requête : une nouvelle valeur = une nouvelle requête. */
  private readonly request = computed(() => ({
    page: this.page(),
    query: this.query(),
    sort: this.sort(),
  }));

  protected readonly view$: Observable<OrderListView> = toObservable(this.request).pipe(
    switchMap(({ page, query, sort }) =>
      this.orderService.listCommandes(page, 20, query, sort).pipe(
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

  protected onSearchInput(text: string): void {
    this.searchText.set(text);
  }

  protected onSortChange(sort: string): void {
    this.sort.set(sort as OrderSort);
  }

  protected previous(): void {
    this.page.update((page) => page - 1);
  }

  protected next(): void {
    this.page.update((page) => page + 1);
  }
}
