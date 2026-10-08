import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Product } from './product.model';

const PRODUCTS: readonly Product[] = [
  /* les 4 produits */ { code: 'BOOK', label: 'Livre', unitPrice: 12.5 },
  { code: 'PEN', label: 'Stylo', unitPrice: 1.2 },
  { code: 'BAG', label: 'Sac à dos', unitPrice: 19.99 },
  { code: 'LAPTOP', label: 'Ordinateur portable', unitPrice: 9950 },
];

@Injectable({ providedIn: 'root' })
export class CatalogService {
  products(): Observable<readonly Product[]> {
    // return of(...)
    return of(PRODUCTS);
  }
}
