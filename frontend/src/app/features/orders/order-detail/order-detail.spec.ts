import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { OrderDetail as Order } from '../order.model';
import { OrderDetail } from './order-detail';

describe('OrderDetail', () => {
  let fixture: ComponentFixture<OrderDetail>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderDetail],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        // la route active : /commandes/abc-123
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ id: 'abc-123' })) },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(OrderDetail);
    page = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('charge la commande dont l’identifiant est dans l’URL, et affiche ses lignes', async () => {
    const order: Order = {
      id: 'abc-123',
      customerId: 'C-001',
      status: 'CREATED',
      totalAmount: 55,
      currency: 'EUR',
      createdAt: '2026-10-10T12:00:00Z',
      lines: [
        { productCode: 'BOOK', quantity: 2, unitPrice: 12.5, lineTotal: 25 },
        { productCode: 'PEN', quantity: 25, unitPrice: 1.2, lineTotal: 30 },
      ],
    };
    http.expectOne('/api/orders/abc-123').flush(order);
    await fixture.whenStable();

    expect(page.querySelectorAll('[data-testid="line"]').length).toBe(2);
    expect(page.querySelector('[data-testid="total"]')?.textContent?.replace(/\D/g, '')).toBe(
      '5500',
    );
  });

  it('affiche « Commande introuvable » si le back répond 404', async () => {
    http.expectOne('/api/orders/abc-123').flush(null, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(page.querySelector('[data-testid="not-found"]')).not.toBeNull();
    expect(page.querySelector('[data-testid="error"]')).toBeNull();
  });

  it('affiche une erreur générique pour toute autre erreur (500)', async () => {
    http
      .expectOne('/api/orders/abc-123')
      .flush(null, { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();

    expect(page.querySelector('[data-testid="error"]')).not.toBeNull();
    expect(page.querySelector('[data-testid="not-found"]')).toBeNull();
  });
});
