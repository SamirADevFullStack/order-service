package com.banque.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banque.order.domain.exception.InvalidOrderException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Test du domaine : Java pur, aucun Spring, aucun mock. Il s'exécute en quelques millisecondes. */
class OrderTest {

    private static final Instant NOW = Instant.parse("2026-10-05T09:00:00Z");

    @Test
    void calcule_le_total_de_la_commande() {
        Order order = Order.create("C-001", List.of(
                new OrderLine("BOOK", 2, Money.of(new BigDecimal("12.50"), "EUR")),
                new OrderLine("PEN", 3, Money.of(new BigDecimal("1.20"), "EUR"))), NOW);

        assertThat(order.total()).isEqualTo(Money.of(new BigDecimal("28.60"), "EUR"));
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void refuse_une_commande_sans_ligne() {
        assertThatThrownBy(() -> Order.create("C-001", List.of(), NOW))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("au moins une ligne");
    }

    @Test
    void refuse_des_lignes_dans_des_devises_differentes() {
        var lines = List.of(
                new OrderLine("BOOK", 1, Money.of(new BigDecimal("10"), "EUR")),
                new OrderLine("PEN", 1, Money.of(new BigDecimal("2"), "USD")));

        assertThatThrownBy(() -> Order.create("C-001", lines, NOW))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void refuse_une_commande_au_dela_du_plafond() {
        var lines = List.of(new OrderLine("GOLD", 1, Money.of(new BigDecimal("10000.01"), "EUR")));

        assertThatThrownBy(() -> Order.create("C-001", lines, NOW))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("dépasser");
    }

    @Test
    void refuse_une_quantite_nulle() {
        assertThatThrownBy(() -> new OrderLine("BOOK", 0, Money.of(BigDecimal.TEN, "EUR")))
                .isInstanceOf(InvalidOrderException.class);
    }
}
