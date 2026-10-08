import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Cart } from './cart';
import { of } from 'rxjs';
import { CatalogService } from './catalog-service';
import { Product } from './product.model';

/** Faux catalogue : le test maîtrise les produits, au lieu de dépendre du vrai service. */
const FAKE_PRODUCTS: readonly Product[] = [
  { code: 'BOOK', label: 'Livre', unitPrice: 12.5 },
  { code: 'PEN', label: 'Stylo', unitPrice: 1.2 },
  { code: 'LAPTOP', label: 'Ordinateur portable', unitPrice: 9950 },
];

describe('Cart', () => {
  let fixture: ComponentFixture<Cart>;
  let page: HTMLElement;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Cart],
      providers: [{ provide: CatalogService, useValue: { products: () => of(FAKE_PRODUCTS) } }],
    }).compileComponents();
    fixture = TestBed.createComponent(Cart);
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  /** Trouve un bouton par son aria-label ou par son texte visible. */
  function button(name: string): HTMLButtonElement {
    const found = Array.from(page.querySelectorAll('button')).find(
      (b) => b.getAttribute('aria-label') === name || b.textContent?.trim() === name,
    );
    if (!found) {
      throw new Error(`Bouton « ${name} » introuvable`);
    }
    return found;
  }

  /** Clique, puis attend que l'écran soit rafraîchi (application zoneless). */
  async function click(name: string): Promise<void> {
    button(name).click();
    await fixture.whenStable();
  }

  /** Les chiffres seuls du total, quel que soit le format : « 9 975,00 € » donne « 997500 ». */
  function totalDigits(): string {
    return page.querySelector('[data-testid="total"]')?.textContent?.replace(/\D/g, '') ?? '';
  }

  function lineCount(): number {
    return page.querySelectorAll('[data-testid="line"]').length;
  }

  it('affiche les deux lignes initiales et leur total (9 975,00 €)', () => {
    expect(lineCount()).toBe(2);
    expect(totalDigits()).toBe('997500');
  });

  it('augmente la quantité et le total quand on clique sur +', async () => {
    await click('Augmenter BOOK');
    expect(page.querySelector('[data-testid="quantity"]')?.textContent?.trim()).toBe('3');
    expect(totalDigits()).toBe('998750');
  });

  it('diminue la quantité et le total quand on clique sur −', async () => {
    await click('Diminuer BOOK');
    expect(page.querySelector('[data-testid="quantity"]')?.textContent?.trim()).toBe('1');
    expect(totalDigits()).toBe('996250');
  });

  it('désactive − quand la quantité vaut 1', () => {
    expect(button('Diminuer LAPTOP').disabled).toBe(true);
    expect(button('Diminuer BOOK').disabled).toBe(false);
  });

  it('désactive + quand une unité de plus dépasserait le plafond', () => {
    expect(button('Augmenter LAPTOP').disabled).toBe(true);
    expect(button('Augmenter BOOK').disabled).toBe(false);
  });

  it("permet d'atteindre exactement 10 000 €, puis bloque +", async () => {
    expect(page.querySelector('[data-testid="max-reached"]')).toBeNull();
    await click('Augmenter BOOK');
    await click('Augmenter BOOK');
    expect(totalDigits()).toBe('1000000');
    expect(page.querySelector('[data-testid="max-reached"]')).not.toBeNull();
    expect(button('Augmenter BOOK').disabled).toBe(true);
  });

  it('retire une ligne', async () => {
    await click('Retirer LAPTOP');
    expect(lineCount()).toBe(1);
    expect(totalDigits()).toBe('2500');
  });

  it('affiche « panier vide » et un total nul après « Vider le panier »', async () => {
    await click('Vider le panier');
    expect(lineCount()).toBe(0);
    expect(page.querySelector('[data-testid="empty"]')).not.toBeNull();
    expect(totalDigits()).toBe('000');
    expect(page.textContent).not.toContain('Vider le panier');
  });

  describe('brouillon dans le localStorage', () => {
    const DRAFT_KEY = 'order-service.cart-draft';

    /** Crée un nouveau panier, comme après un rechargement de la page (F5). */
    async function reloadCart(): Promise<HTMLElement> {
      const reloaded = TestBed.createComponent(Cart);
      await reloaded.whenStable();
      return reloaded.nativeElement as HTMLElement;
    }

    it('sauvegarde le panier après chaque modification', async () => {
      await click('Augmenter BOOK');
      const saved = JSON.parse(localStorage.getItem(DRAFT_KEY) ?? '[]');
      expect(saved).toEqual([
        { productCode: 'BOOK', quantity: 3, unitPrice: 12.5 },
        { productCode: 'LAPTOP', quantity: 1, unitPrice: 9950 },
      ]);
    });

    it('restaure le panier au rechargement de la page', async () => {
      await click('Retirer LAPTOP');
      const reloaded = await reloadCart();
      expect(reloaded.querySelectorAll('[data-testid="line"]').length).toBe(1);
      expect(reloaded.textContent).toContain('BOOK');
      expect(reloaded.textContent).not.toContain('LAPTOP');
    });

    it('restaure un panier vidé comme un panier vide, pas comme le panier de départ', async () => {
      await click('Vider le panier');
      const reloaded = await reloadCart();
      expect(reloaded.querySelector('[data-testid="empty"]')).not.toBeNull();
    });

    it('ignore un brouillon illisible et repart du panier de départ', async () => {
      localStorage.setItem(DRAFT_KEY, '{pas du JSON');
      const reloaded = await reloadCart();
      expect(reloaded.querySelectorAll('[data-testid="line"]').length).toBe(2);
    });
  });

  describe('ajout depuis le catalogue', () => {
    /** Choisit un produit dans la liste déroulante, comme le ferait l'utilisateur. */
    async function selectProduct(code: string): Promise<void> {
      const select = page.querySelector<HTMLSelectElement>('select[aria-label="Produit"]');
      if (!select) {
        throw new Error('Liste des produits introuvable');
      }
      select.value = code;
      select.dispatchEvent(new Event('change'));
      await fixture.whenStable();
    }

    it('propose les produits du catalogue', () => {
      const codes = Array.from(page.querySelectorAll<HTMLOptionElement>('select option'))
        .map((option) => option.value)
        .filter((value) => value !== '');
      expect(codes).toEqual(['BOOK', 'PEN', 'LAPTOP']);
    });

    it("désactive « Ajouter » tant qu'aucun produit n'est choisi", () => {
      expect(button('Ajouter').disabled).toBe(true);
    });

    it('ajoute un produit absent du panier avec une quantité de 1', async () => {
      await selectProduct('PEN');
      await click('Ajouter');
      expect(lineCount()).toBe(3);
      expect(totalDigits()).toBe('997620');
    });

    it('additionne la quantité si le produit est déjà dans le panier', async () => {
      await selectProduct('BOOK');
      await click('Ajouter');
      expect(lineCount()).toBe(2);
      expect(page.querySelector('[data-testid="quantity"]')?.textContent?.trim()).toBe('3');
    });

    it('désactive « Ajouter » si une unité du produit dépasserait le plafond', async () => {
      await selectProduct('LAPTOP');
      expect(button('Ajouter').disabled).toBe(true);
    });
  });
});
