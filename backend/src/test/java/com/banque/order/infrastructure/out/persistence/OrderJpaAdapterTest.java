package com.banque.order.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.pagination.PageResult;
import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Test d'intégration de l'adaptateur : vraie base H2 en mémoire, vraies requêtes SQL. */
@DataJpaTest
@Import({OrderJpaAdapter.class, OrderPersistenceMapper.class})
class OrderJpaAdapterTest {

    @Autowired
    private OrderJpaAdapter adapter;

    private Order orderCreatedAt(String instant) {
        return Order.create("C-001",
                List.of(new OrderLine("BOOK", 1, Money.of(new BigDecimal("12.50"), "EUR"))),
                Instant.parse(instant));
    }

    @Test
    void pagine_du_plus_recent_au_plus_ancien() {
        // given : 3 commandes enregistrées DANS LE DÉSORDRE
        Order oldest = adapter.save(orderCreatedAt("2026-10-01T10:00:00Z"));
        Order newest = adapter.save(orderCreatedAt("2026-10-03T10:00:00Z"));
        Order middle = adapter.save(orderCreatedAt("2026-10-02T10:00:00Z"));

        // when : deux pages de 2
        PageResult<Order> first = adapter.findAll(new PageQuery(0, 2));
        PageResult<Order> second = adapter.findAll(new PageQuery(1, 2));

        // then
        assertThat(first.content()).extracting(Order::id).containsExactly(newest.id(), middle.id());
        assertThat(second.content()).extracting(Order::id).containsExactly(oldest.id());
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
    }

    @Test
    void renvoie_les_lignes_de_chaque_commande() {
        adapter.save(orderCreatedAt("2026-10-01T10:00:00Z"));

        Order found = adapter.findAll(new PageQuery(0, 20)).content().getFirst();

        assertThat(found.lines()).hasSize(1);
        assertThat(found.total()).isEqualTo(Money.of(new BigDecimal("12.50"), "EUR"));
    }
}