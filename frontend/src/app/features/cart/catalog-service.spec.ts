import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { CatalogService } from './catalog-service';

describe('CatalogService', () => {
  it('fournit les 4 produits du catalogue', async () => {
    const products = await firstValueFrom(TestBed.inject(CatalogService).products());
    expect(products.map((p) => p.code)).toEqual(['BOOK', 'PEN', 'BAG', 'LAPTOP']);
  });
});
