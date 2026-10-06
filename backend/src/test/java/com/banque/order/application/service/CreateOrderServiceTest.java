package com.banque.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.banque.order.application.port.in.CreateOrderCommand;
import com.banque.order.application.port.out.EventPublisher;
import com.banque.order.application.port.out.OrderRepository;
import com.banque.order.domain.exception.InvalidOrderException;
import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderCreatedEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Test du cas d'usage : on mocke les PORTS SORTANTS, pas besoin de base ni de Kafka. */
@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T09:00:00Z");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisher eventPublisher;

    private CreateOrderService service;

    @BeforeEach
    void setUp() {
        service = new CreateOrderService(orderRepository, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void enregistre_la_commande_puis_publie_l_evenement() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var command = new CreateOrderCommand("C-001", "EUR",
                List.of(new CreateOrderCommand.LineItem("BOOK", 2, new BigDecimal("12.50"))));

        Order order = service.create(command);

        assertThat(order.total()).isEqualTo(Money.of(new BigDecimal("25.00"), "EUR"));

        InOrder inOrder = inOrder(orderRepository, eventPublisher);
        inOrder.verify(orderRepository).save(any(Order.class));
        ArgumentCaptor<OrderCreatedEvent> event = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        inOrder.verify(eventPublisher).publish(event.capture());

        assertThat(event.getValue().orderId()).isEqualTo(order.id());
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void ne_sauvegarde_ni_ne_publie_si_la_commande_est_invalide() {
        var command = new CreateOrderCommand("C-001", "EUR", List.of());

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(InvalidOrderException.class);

        verifyNoInteractions(orderRepository, eventPublisher);
    }
}
