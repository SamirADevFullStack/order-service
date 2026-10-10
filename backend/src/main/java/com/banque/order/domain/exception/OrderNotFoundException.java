package com.banque.order.domain.exception;

import com.banque.order.domain.model.OrderId;

/** Aucune commande ne porte cet identifiant. C'est l'adaptateur REST qui la traduira en 404. */
/** RuntimeException (non vérifiée), comme InvalidOrderException : l’appelant n’est pas obligé de la déclarer avec throws, et elle remonte jusqu’à RestExceptionHandler ; */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(OrderId id) {
        // à toi : appeler le constructeur parent avec un message qui contient l'identifiant (id.value())
        super("Aucune commande ne porte cet identifiant: " + id);
    }
}