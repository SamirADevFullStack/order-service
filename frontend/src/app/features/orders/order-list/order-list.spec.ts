import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderPage } from '../order.model';
import { OrderList } from './order-list';

describe('OrderList', () => {
  let fixture: ComponentFixture<OrderList>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderList],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(OrderList);
    page = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('affiche « Chargement » tant que la réponse n’est pas arrivée', () => {
    expect(page.querySelector('[data-testid="loading"]')).not.toBeNull();
    http.expectOne('/api/orders?page=0&size=20');
  });

  it('affiche les commandes reçues du back', async () => {
    const body: OrderPage = {
      content: [
        {
          id: 'a',
          status: 'CREATED',
          totalAmount: 25,
          currency: 'EUR',
          createdAt: '2026-10-10T12:00:00Z',
        },
        {
          id: 'b',
          status: 'CREATED',
          totalAmount: 9.9,
          currency: 'EUR',
          createdAt: '2026-10-09T12:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    };
    http.expectOne('/api/orders?page=0&size=20').flush(body);
    await fixture.whenStable();

    expect(page.querySelectorAll('[data-testid="order"]').length).toBe(2);
    expect(page.querySelector('[data-testid="count"]')?.textContent).toContain('2 commandes');
  });

  it('affiche un message quand il n’y a aucune commande', async () => {
    http
      .expectOne('/api/orders?page=0&size=20')
      .flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    await fixture.whenStable();

    expect(page.querySelector('[data-testid="empty"]')).not.toBeNull();
  });

  it('affiche un message d’erreur si le back ne répond pas', async () => {
    http
      .expectOne('/api/orders?page=0&size=20')
      .flush(null, { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();

    expect(page.querySelector('[data-testid="error"]')?.textContent).toContain(
      'Impossible de charger',
    );
  });
});
