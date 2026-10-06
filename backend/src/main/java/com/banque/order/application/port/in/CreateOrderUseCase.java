package com.banque.order.application.port.in;

import com.banque.order.domain.model.Order;

/**
 * PORT ENTRANT : ce que l'application sait faire.
 * Les adaptateurs entrants (contrôleur REST, consumer Kafka) appellent cette interface,
 * jamais directement l'implémentation.
 */
public interface CreateOrderUseCase {

    Order create(CreateOrderCommand command);
}
