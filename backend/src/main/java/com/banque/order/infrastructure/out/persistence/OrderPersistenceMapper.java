package com.banque.order.infrastructure.out.persistence;

import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderId;
import com.banque.order.domain.model.OrderLine;
import com.banque.order.domain.model.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * MAPPER de l'adaptateur de persistance : objet du domaine <-> entité JPA.
 * C'est le prix à payer pour garder un domaine sans annotation JPA.
 */
@Component
public class OrderPersistenceMapper {

    public OrderJpaEntity toEntity(Order order) {
        var lines = order.lines().stream()
                .map(line -> new OrderLineJpaEntity(line.productCode(), line.quantity(), line.unitPrice().amount()))
                .toList();
        return new OrderJpaEntity(
                order.id().value(),
                order.customerId(),
                order.status().name(),
                order.total().currency(),
                order.createdAt(),
                lines);
    }

    public Order toDomain(OrderJpaEntity entity) {
        var lines = entity.getLines().stream()
                .map(line -> new OrderLine(line.getProductCode(), line.getQuantity(),
                        Money.of(line.getUnitPrice(), entity.getCurrency())))
                .toList();
        // restore() et non create() : on reconstruit une commande existante sans rejouer la création.
        return Order.restore(
                new OrderId(entity.getId()),
                entity.getCustomerId(),
                lines,
                OrderStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }
}
