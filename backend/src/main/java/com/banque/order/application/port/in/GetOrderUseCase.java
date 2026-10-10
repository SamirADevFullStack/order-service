package com.banque.order.application.port.in;

import com.banque.order.domain.exception.OrderNotFoundException;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderId;

/** PORT ENTRANT : consulter une commande. */
public interface GetOrderUseCase {

    /** @throws OrderNotFoundException si aucune commande ne porte cet identifiant */
    Order get(OrderId id);
}