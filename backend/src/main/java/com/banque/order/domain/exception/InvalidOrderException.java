package com.banque.order.domain.exception;

/**
 * Exception métier : levée quand une règle de gestion de la commande n'est pas respectée.
 * Elle ne connaît ni HTTP ni Kafka : c'est l'adaptateur REST qui la traduira en 400.
 */
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
