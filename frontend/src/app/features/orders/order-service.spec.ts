import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { OrderService } from './order-service';
import { OrderPage } from './order.model';

describe('OrderService', () => {
  let service: OrderService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrderService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // aucune requête inattendue ne doit rester en attente
    http.verify();
  });

  it('demande une page de commandes avec les paramètres page et size', async () => {
    const page: OrderPage = { content: [], page: 1, size: 20, totalElements: 25, totalPages: 2 };

    const result = firstValueFrom(service.listCommandes(1, 20));
    const request = http.expectOne('/api/orders?page=1&size=20');
    expect(request.request.method).toBe('GET');
    request.flush(page);

    expect(await result).toEqual(page);
  });

  it('demande le détail d’une commande par son identifiant', async () => {
    const result = firstValueFrom(service.getCommande('abc-123'));
    http.expectOne('/api/orders/abc-123').flush({ id: 'abc-123', lines: [] });

    expect((await result).id).toBe('abc-123');
  });

  it("n'envoie aucune requête tant que personne ne s'abonne", () => {
    service.listCommandes();
    http.expectNone('/api/orders?page=0&size=20');
  });
});
