package com.banque.order.application.search;

/**
 * Les critères d'une recherche de commandes. Tous facultatifs.
 * text : cherché dans l'identifiant du client OU dans le code d'un produit commandé (sans tenir compte des majuscules).
 */
public record OrderSearchCriteria(String text, SortOrder sort) {

    public OrderSearchCriteria {
        text = (text == null || text.isBlank()) ? null : text.trim();
        sort = (sort == null) ? SortOrder.NEWEST_FIRST : sort;
    }

    /** Aucun filtre, ordre par défaut : les plus récentes d'abord. */
    public static OrderSearchCriteria none() {
        return new OrderSearchCriteria(null, null);
    }

    public boolean hasText() {
        return text != null;
    }
}
