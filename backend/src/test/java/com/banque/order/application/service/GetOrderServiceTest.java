package com.banque.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.banque.order.application.port.out.OrderRepository;
import com.banque.order.domain.exception.OrderNotFoundException;
import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderId;
import com.banque.order.domain.model.OrderLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private GetOrderService service;
/*
    @BeforeEach
    void setUp() {
        service = new GetOrderService(orderRepository);
    }
*/

    @Test
    void renvoie_la_commande_trouvee() {
        // given : une vraie commande du domaine, que le port sortant renverra
        Order order = Order.create("C-001",
                List.of(new OrderLine("BOOK", 1, Money.of(new BigDecimal("12.50"), "EUR"))), Instant.now());
        // à toi : programmer le mock pour que findById(order.id()) renvoie Optional.of(order)
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        // when / then
        Order result = service.get(order.id());
        // à toi : vérifier que service.get(order.id()) renvoie bien cette commande
        assertThat(result).isSameAs(order);
    }

    @Test
    void leve_une_exception_metier_si_la_commande_n_existe_pas() {
        // given : un identifiant inconnu, le port sortant renvoie un Optional vide
        OrderId unknown = OrderId.newId();
        // programmer le mock pour que findById(unknown) renvoie Optional.empty()
        when(orderRepository.findById(unknown)).thenReturn(Optional.empty());
        // when / then
        assertThatThrownBy(() -> service.get(unknown))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(unknown.value().toString());
    }
}