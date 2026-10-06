package com.banque.order.application.service;

import com.banque.order.application.port.in.CreateOrderCommand;
import com.banque.order.application.port.in.CreateOrderUseCase;
import com.banque.order.application.port.out.EventPublisher;
import com.banque.order.application.port.out.OrderRepository;
import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderCreatedEvent;
import com.banque.order.domain.model.OrderLine;
import java.time.Clock;
import java.util.List;

/**
 * IMPLÉMENTATION DU CAS D'USAGE : elle orchestre, elle ne contient pas les règles métier.
 * 1. traduit la commande en objets du domaine,
 * 2. laisse le domaine appliquer ses règles (Order.create),
 * 3. appelle les ports sortants pour sauvegarder puis publier.
 * Aucune annotation Spring : le bean est déclaré dans infrastructure/config.
 */
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public CreateOrderService(OrderRepository orderRepository, EventPublisher eventPublisher, Clock clock) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public Order create(CreateOrderCommand command) {
        List<OrderLine> lines = command.lines() == null ? List.of() : command.lines().stream()
                .map(item -> new OrderLine(item.productCode(), item.quantity(),
                        Money.of(item.unitPrice(), command.currency())))
                .toList();

        Order order = Order.create(command.customerId(), lines, clock.instant());

        Order saved = orderRepository.save(order);
        eventPublisher.publish(OrderCreatedEvent.from(saved));
        return saved;
    }
}
