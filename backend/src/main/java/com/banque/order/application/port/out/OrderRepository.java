package com.banque.order.application.port.out;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.pagination.PageResult;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderId;
import java.util.Optional;

/**
 * PORT SORTANT : ce dont l'application a besoin pour stocker ses commandes.
 * L'interface est définie ici, côté cœur. L'adaptateur JPA l'implémente côté infrastructure :
 * c'est l'inversion de dépendance.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId id);

    public PageResult<Order> findAll(PageQuery query);
}
