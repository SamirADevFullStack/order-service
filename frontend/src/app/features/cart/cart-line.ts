export interface CartLine {
  // les 3 champs d'OrderLineRequest (contrat OpenAPI)
  productCode: string;
  quantity: number;
  unitPrice: number;
}

export const MAX_TOTAL_CENTS = 1_000_000; // 10 000,00 € : même plafond que Order.MAX_TOTAL côté back
export function lineTotalCents(line: CartLine): number {
  // prix en centimes arrondi, puis multiplié par la quantité
  return Math.round(line.unitPrice * 100) * line.quantity;
}

export function totalCents(lines: readonly CartLine[]): number {
  return lines.reduce((sum, line) => sum + lineTotalCents(line), 0);
}

export function exceedsMax(amountCents: number): boolean {
  // même borne que Order.create côté back
  return amountCents > MAX_TOTAL_CENTS;
}
