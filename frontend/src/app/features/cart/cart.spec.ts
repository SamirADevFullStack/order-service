import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Cart } from './cart';

describe('Cart', () => {
  let fixture: ComponentFixture<Cart>;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Cart] }).compileComponents();
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
});