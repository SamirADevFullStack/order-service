package com.banque.order.domain.model;

import com.banque.order.domain.exception.InvalidOrderException;

/**
 * Value object : une ligne de commande (un produit, une quantité, un prix unitaire).
 * Elle n'existe qu'à l'intérieur de sa commande.
 */
public record OrderLine(String productCode, int quantity, Money unitPrice) {

    public OrderLine {
        if (productCode == null || productCode.isBlank()) {
            throw new InvalidOrderException("Le code produit est obligatoire");
        }
        if (quantity <= 0) {
            throw new InvalidOrderException("La quantité doit être positive");
        }
        if (unitPrice == null) {
            throw new InvalidOrderException("Le prix unitaire est obligatoire");
        }
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
