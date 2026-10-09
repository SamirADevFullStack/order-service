import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';
import { loadDraft, saveDraft } from './cart-draft';
import { CartLine, MAX_TOTAL_CENTS, exceedsMax, lineTotalCents, totalCents } from './cart-line';
import { CatalogService } from './catalog-service';

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

  // l'état : un signal de lignes, initialisé avec INITIAL_LINES
  protected readonly lines = signal<readonly CartLine[]>(loadDraft(localStorage) ?? INITIAL_LINES);

  protected readonly quantity = linkedSignal({
    // le signal surveillé
    source: this.selectedCode,
    // la valeur à prendre quand la source change
    computation: () => 1,
  });

  protected readonly selectedProduct = computed(() =>
    this.products().find((product) => product.code === this.selectedCode()),
  );

  protected readonly canAdd = computed(() => {
    const product = this.selectedProduct();
    if (!product) {
      return false;
    }

    const addedCents = lineTotalCents({
      productCode: product.code,
      quantity: this.quantity(),
      unitPrice: product.unitPrice,
    });

    return !exceedsMax(this.total() + addedCents);
  });

  constructor() {
    toObservable(this.lines) // le signal devient un flux
      .pipe(debounceTime(500)) // on attend 500 ms de calme
      .subscribe((lines) => saveDraft(localStorage, lines)); // puis on sauvegarde
  }

  // constructor() {
  //   effect(() =>
  //     /* sauvegarder this.lines() dans localStorage */
  //     saveDraft(localStorage, this.lines()),
  //   );
  // }

  protected add(): void {
    const product = this.selectedProduct();
    if (!product) {
      return;
    }

    /* une ligne a-t-elle déjà ce productCode ? */
    const alreadyInCart = this.lines().some((line) => line.productCode === product.code);

    if (alreadyInCart) {
      this.addQuantity(product.code, this.quantity());
    } else {
      this.lines.update((lines) => [
        ...lines,
        { productCode: product.code, quantity: this.quantity(), unitPrice: product.unitPrice },
      ]);
    }
  }

  private addQuantity(productCode: string, quantity: number): void {
    // le même map qu'increment, mais avec + quantity au lieu de + 1
    this.lines.update((lines) =>
      lines.map((line) =>
        line.productCode === productCode ? { ...line, quantity: line.quantity + quantity } : line,
      ),
    );
  }

  protected onQuantityInput(value: number): void {
    // si value est un entier (Number.isInteger) ET >= 1 → this.quantity.set(value)
    if (Number.isInteger(value) && value >= 1) {
      this.quantity.set(value);
    } else {
      // sinon → ne rien faire (on garde la dernière quantité valide)
      return;
    }
  }

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
    this.addQuantity(productCode, 1);
  }
  protected decrement(productCode: string): void {
    this.addQuantity(productCode, -1);
  }

  // protected increment(productCode: string): void {
  //   // update + map : la ligne concernée devient une NOUVELLE ligne avec quantity + 1
  //   this.lines.update((lines) =>
  //     lines.map((line) =>
  //       line.productCode === productCode ? { ...line, quantity: line.quantity + 1 } : line,
  //     ),
  //   );
  // }

  // protected decrement(productCode: string): void {
  //   // même chose avec quantity - 1
  //   this.lines.update((lines) =>
  //     lines.map((line) =>
  //       line.productCode === productCode ? { ...line, quantity: line.quantity - 1 } : line,
  //     ),
  //   );
  // }

  protected remove(productCode: string): void {
    // update + filter : garder les lignes dont le productCode est différent
    this.lines.update((lines) => lines.filter((line) => line.productCode !== productCode));
  }

  protected clear(): void {
    this.lines.set([]);
  }
}
