import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { Cart } from './features/cart/cart';

describe('routes', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
  });

  it('redirige la racine vers /panier', async () => {
    await RouterTestingHarness.create('/');
    expect(TestBed.inject(Router).url).toBe('/panier');
  });

  it('redirige une URL inconnue vers /panier', async () => {
    await RouterTestingHarness.create('/nimporte-quoi');
    expect(TestBed.inject(Router).url).toBe('/panier');
  });

  it('affiche le composant Cart sur /panier', async () => {
    const harness = await RouterTestingHarness.create();
    const cart = await harness.navigateByUrl('/panier', Cart);
    expect(cart).toBeInstanceOf(Cart);
  });
});
