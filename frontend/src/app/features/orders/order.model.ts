/** Reflet du contrat OpenAPI (order-api.yaml) : mêmes noms, mêmes champs obligatoires. */

export interface OrderSummary {
  id: string;
  status: string;
  totalAmount: number;
  currency: string;
  /** Date ISO 8601 (JSON n'a pas de type date) : « 2026-10-10T12:32:00Z ». */
  createdAt: string;
}

export interface OrderPage {
  content: OrderSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface OrderLine {
  productCode: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderDetail {
  id: string;
  customerId: string;
  status: string;
  totalAmount: number;
  currency: string;
  createdAt: string;
  lines: OrderLine[];
}
