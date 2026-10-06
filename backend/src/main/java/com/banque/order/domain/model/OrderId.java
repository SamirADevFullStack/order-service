package com.banque.order.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object : l'identifiant d'une commande.
 * Plutôt qu'un UUID « nu », un type dédié évite de confondre un id de commande avec un autre id.
 */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "L'identifiant de commande est obligatoire");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
