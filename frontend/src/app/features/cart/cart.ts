import { CurrencyPipe } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { CartLine, MAX_TOTAL_CENTS, exceedsMax, lineTotalCents, totalCents } from './cart-line';

/** Panier de départ : 9 975,00 €. Deux BOOK de plus atteignent exactement le plafond. */
const INITIAL_LINES: readonly CartLine[] = [
  { productCode: 'BOOK', quantity: 2, unitPrice: 12.5 },
  { productCode: 'LAPTOP', quantity: 1, unitPrice: 9950 },
];

@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
})
export class Cart {
  // l'état : un signal de lignes, initialisé avec INITIAL_LINES
  protected readonly lines = signal<readonly CartLine[]>(INITIAL_LINES);

  // deux computed, en centimes : le total
  protected readonly total = computed<number>(() => {
    return totalCents(this.lines());
  });

  // Ce qu'il reste avant le plafond
  protected readonly remaining = computed(() => MAX_TOTAL_CENTS - this.total());

  // le template ne voit que les membres du composant : on lui expose la fonction importée
  protected readonly lineTotalCents = lineTotalCents;

  protected canIncrement(line: CartLine): boolean {
    // ajouter UNE unité de cette ligne au total dépasserait-il le plafond ?
    // indice : exceedsMax et lineTotalCents({ ...line, quantity: 1 })
    const unitCents = lineTotalCents({ ...line, quantity: 1 });
    return !exceedsMax(this.total() + unitCents);
  }

  protected increment(productCode: string): void {
    // update + map : la ligne concernée devient une NOUVELLE ligne avec quantity + 1
    this.lines.update((lines) =>
      lines.map((line) =>
        line.productCode === productCode ? { ...line, quantity: line.quantity + 1 } : line,
      ),
    );
  }

  protected decrement(productCode: string): void {
    // même chose avec quantity - 1
    this.lines.update((lines) =>
      lines.map((line) =>
        line.productCode === productCode ? { ...line, quantity: line.quantity - 1 } : line,
      ),
    );
  }

  protected remove(productCode: string): void {
    // update + filter : garder les lignes dont le productCode est différent
    this.lines.update((lines) => lines.filter((line) => line.productCode !== productCode));
  }

  protected clear(): void {
    this.lines.set([]);
  }
}
