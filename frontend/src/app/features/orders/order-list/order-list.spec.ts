import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { OrderPage } from '../order.model';
import { OrderList } from './order-list';

describe('OrderList', () => {
  let fixture: ComponentFixture<OrderList>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderList],
      // RouterLink (les lignes sont des liens vers le détail) a besoin du routeur
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(OrderList);
    page = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
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

  describe('pagination', () => {
    function pageOf(page: number, totalElements: number): OrderPage {
      return {
        content: [
          {
            id: 'p' + page,
            status: 'CREATED',
            totalAmount: 10,
            currency: 'EUR',
            createdAt: '2026-10-10T12:00:00Z',
          },
        ],
        page,
        size: 20,
        totalElements,
        totalPages: Math.ceil(totalElements / 20),
      };
    }

    function button(name: string): HTMLButtonElement {
      const found = Array.from(page.querySelectorAll('button')).find(
        (b) => b.textContent?.trim() === name,
      );
      if (!found) {
        throw new Error(`Bouton « ${name} » introuvable`);
      }
      return found;
    }

    it('charge la page suivante quand on clique sur « Suivant »', async () => {
      http.expectOne('/api/orders?page=0&size=20').flush(pageOf(0, 25));
      await fixture.whenStable();
      expect(page.querySelector('[data-testid="page-info"]')?.textContent).toContain(
        'Page 1 sur 2',
      );
      expect(button('Précédent').disabled).toBe(true);

      button('Suivant').click();
      await fixture.whenStable();
      http.expectOne('/api/orders?page=1&size=20').flush(pageOf(1, 25));
      await fixture.whenStable();

      expect(page.querySelector('[data-testid="page-info"]')?.textContent).toContain(
        'Page 2 sur 2',
      );
      expect(button('Suivant').disabled).toBe(true);
    });

    it("annule la requête précédente si l'utilisateur change de page avant la réponse (switchMap)", async () => {
      http.expectOne('/api/orders?page=0&size=20').flush(pageOf(0, 65));
      await fixture.whenStable();

      button('Suivant').click();
      await fixture.whenStable();
      const pageTwo = http.expectOne('/api/orders?page=1&size=20');
      button('Suivant').click();
      await fixture.whenStable();
      http.expectOne('/api/orders?page=2&size=20').flush(pageOf(2, 65));
      await fixture.whenStable();

      expect(pageTwo.cancelled).toBe(true);
      expect(page.querySelector('[data-testid="page-info"]')?.textContent).toContain(
        'Page 3 sur 4',
      );
    });

    it('reste utilisable après une erreur : la page suivante se charge encore', async () => {
      http.expectOne('/api/orders?page=0&size=20').flush(pageOf(0, 45));
      await fixture.whenStable();

      button('Suivant').click();
      await fixture.whenStable();
      http
        .expectOne('/api/orders?page=1&size=20')
        .flush(null, { status: 500, statusText: 'Erreur' });
      await fixture.whenStable();
      expect(page.querySelector('[data-testid="error"]')).not.toBeNull();

      fixture.componentInstance['page'].set(2);
      await fixture.whenStable();
      http.expectOne('/api/orders?page=2&size=20').flush(pageOf(2, 45));
      await fixture.whenStable();

      expect(page.querySelector('[data-testid="page-info"]')?.textContent).toContain(
        'Page 3 sur 3',
      );
    });
  });
});
