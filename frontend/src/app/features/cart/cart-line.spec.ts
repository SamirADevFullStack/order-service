import { CartLine, exceedsMax, lineTotalCents, totalCents } from './cart-line';

describe('cart-line', () => {
  describe('lineTotalCents', () => {
    it('multiplie le prix unitaire par la quantité, en centimes', () => {
      expect(lineTotalCents({ productCode: 'BOOK', quantity: 2, unitPrice: 12.5 })).toBe(2500);
    });

    it('arrondit correctement un prix non représentable exactement (19,99 €)', () => {
      // En JavaScript, 19.99 * 100 vaut 1998.9999999999998
      expect(lineTotalCents({ productCode: 'BAG', quantity: 1, unitPrice: 19.99 })).toBe(1999);
    });

    it('évite les erreurs de virgule flottante sur la multiplication (3 × 0,07 €)', () => {
      // En JavaScript, 0.07 * 3 * 100 vaut 21.000000000000004
      expect(lineTotalCents({ productCode: 'CLIP', quantity: 3, unitPrice: 0.07 })).toBe(21);
    });
  });

  describe('totalCents', () => {
    it('vaut 0 pour un panier vide', () => {
      expect(totalCents([])).toBe(0);
    });

    it('additionne les lignes', () => {
      const lines: CartLine[] = [
        { productCode: 'BOOK', quantity: 2, unitPrice: 12.5 },
        { productCode: 'PEN', quantity: 3, unitPrice: 1.2 },
      ];
      expect(totalCents(lines)).toBe(2860);
    });

    it('additionne sans erreur de virgule flottante (0,10 € + 0,20 €)', () => {
      // En JavaScript, 0.1 + 0.2 vaut 0.30000000000000004
      const lines: CartLine[] = [
        { productCode: 'A', quantity: 1, unitPrice: 0.1 },
        { productCode: 'B', quantity: 1, unitPrice: 0.2 },
      ];
      expect(totalCents(lines)).toBe(30);
    });
  });

  describe('exceedsMax', () => {
    it('accepte exactement 10 000 €, comme Order.create côté back', () => {
      expect(exceedsMax(1_000_000)).toBe(false);
    });

    it('refuse 10 000,01 €', () => {
      expect(exceedsMax(1_000_001)).toBe(true);
    });
  });
});
