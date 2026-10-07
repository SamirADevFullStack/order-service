import { CartLine } from './cart-line';

export const DRAFT_KEY = 'order-service.cart-draft';

/** Le panier sauvegardé, ou null s'il n'y en a pas ou s'il est illisible. */
export function loadDraft(storage: Storage): readonly CartLine[] | null {
  // 1. lire la chaîne : storage.getItem(DRAFT_KEY) → string, ou null si absente
  const json = storage.getItem(DRAFT_KEY);
  // 2. si null → renvoyer null
  if (json === null) return null;

  // 3. la transformer en objet : JSON.parse(json) — attention, LÈVE UNE EXCEPTION si le JSON est invalide
  try {
    // 4. si le résultat est un tableau (Array.isArray) → le renvoyer, sinon → null
    const parsed: unknown = JSON.parse(json);
    if (Array.isArray(parsed)) {
      return parsed as CartLine[];
    }
    return null;
  } catch {
    return null;
  }
}

/** Sauvegarde le panier. */
export function saveDraft(storage: Storage, lines: readonly CartLine[]): void {
  // transformer le tableau en chaîne (JSON.stringify), puis storage.setItem(DRAFT_KEY, chaîne)
  try {
    storage.setItem(DRAFT_KEY, JSON.stringify(lines));
  } catch {
    // stockage plein ou indisponible : le panier fonctionne quand même, il n'est juste pas sauvegardé
  }
}
