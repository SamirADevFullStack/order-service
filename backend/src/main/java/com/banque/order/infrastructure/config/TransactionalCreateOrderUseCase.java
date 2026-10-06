package com.banque.order.infrastructure.config;

import com.banque.order.application.port.in.CreateOrderCommand;
import com.banque.order.application.port.in.CreateOrderUseCase;
import com.banque.order.domain.model.Order;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DÉCORATEUR TRANSACTIONNEL : exécute tout le cas d'usage dans UNE seule transaction.
 * La sauvegarde de la commande ET l'écriture dans la table outbox sont donc validées ensemble, ou annulées ensemble.
 *
 * Pourquoi un décorateur plutôt qu'un @Transactional sur CreateOrderService ?
 * Pour garder la couche application sans aucune dépendance à Spring (le test ArchUnit le vérifie).
 * La transaction est une préoccupation technique : elle reste dans l'infrastructure.
 */
public class TransactionalCreateOrderUseCase implements CreateOrderUseCase {

    private final CreateOrderUseCase delegate;
    private final TransactionTemplate transactionTemplate;

    public TransactionalCreateOrderUseCase(CreateOrderUseCase delegate, TransactionTemplate transactionTemplate) {
        this.delegate = delegate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Order create(CreateOrderCommand command) {
        // Une RuntimeException (ex. InvalidOrderException) provoque un rollback, puis elle est relancée telle quelle.
        return transactionTemplate.execute(status -> delegate.create(command));
    }
}
