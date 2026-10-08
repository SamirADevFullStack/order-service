import { CurrencyPipe } from '@angular/common';
import { CartLine, MAX_TOTAL_CENTS, exceedsMax, lineTotalCents, totalCents } from './cart-line';
import { DRAFT_KEY, loadDraft, saveDraft } from './cart-draft';
import { CatalogService } from './catalog-service';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

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
  private readonly catalog = inject(CatalogService);
  protected readonly products = toSignal(this.catalog.products(), { initialValue: [] });
  protected readonly selectedCode = signal('');

  protected readonly selectedProduct = computed(() =>
    this.products().find((product) => product.code === this.selectedCode()),
  );

  protected readonly canAdd = computed(() => {
    const product = this.selectedProduct();
    if (!product) {
      return false;
    }
    // ici, TypeScript sait que product n'est plus undefined
    // → calculer le prix d'une unité en centimes, puis vérifier le plafond
    const unitCents = lineTotalCents({
      productCode: product.code,
      quantity: 1,
      unitPrice: product.unitPrice,
    });

    return !exceedsMax(this.total() + unitCents);
  });

  protected add(): void {
    const product = this.selectedProduct();
    if (!product) {
      return;
    }

    /* une ligne a-t-elle déjà ce productCode ? */
    const alreadyInCart = this.lines().some((line) => line.productCode === product.code);

    if (alreadyInCart) {
      this.increment(product.code);
    } else {
      this.lines.update((lines) => [
        ...lines,
        { productCode: product.code, quantity: 1, unitPrice: product.unitPrice },
      ]);
    }
  }

  // l'état : un signal de lignes, initialisé avec INITIAL_LINES
  protected readonly lines = signal<readonly CartLine[]>(loadDraft(localStorage) ?? INITIAL_LINES);

  // deux computed, en centimes : le total
  protected readonly total = computed<number>(() => {
    return totalCents(this.lines());
  });

  // Ce qu'il reste avant le plafond
  protected readonly remaining = computed(() => MAX_TOTAL_CENTS - this.total());

  // le template ne voit que les membres du composant : on lui expose la fonction importée
  protected readonly lineTotalCents = lineTotalCents;

  constructor() {
    effect(() =>
      /* à compléter : sauvegarder this.lines() dans localStorage */
      saveDraft(localStorage, this.lines()),
    );
  }

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
