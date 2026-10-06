package com.banque.order.domain.model;

import com.banque.order.domain.exception.InvalidOrderException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object : un montant dans une devise.
 * Immuable, sans identité, comparé par sa valeur. Il protège ses propres règles
 * (pas de montant négatif, pas d'addition entre deux devises différentes).
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        if (amount == null || currency == null) {
            throw new InvalidOrderException("Le montant et la devise sont obligatoires");
        }
        if (amount.signum() < 0) {
            throw new InvalidOrderException("Un montant ne peut pas être négatif");
        }
        if (!currency.matches("[A-Z]{3}")) {
            throw new InvalidOrderException("Devise invalide : " + currency);
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new InvalidOrderException("Impossible d'additionner " + currency + " et " + other.currency);
        }
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }
}
