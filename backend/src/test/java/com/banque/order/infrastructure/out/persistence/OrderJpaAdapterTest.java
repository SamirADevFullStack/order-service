package com.banque.order.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.pagination.PageResult;
import com.banque.order.application.search.OrderSearchCriteria;
import com.banque.order.application.search.SortOrder;
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
        PageResult<Order> first = adapter.findAll(OrderSearchCriteria.none(), new PageQuery(0, 2));
        PageResult<Order> second = adapter.findAll(OrderSearchCriteria.none(), new PageQuery(1, 2));

        // then
        assertThat(first.content()).extracting(Order::id).containsExactly(newest.id(), middle.id());
        assertThat(second.content()).extracting(Order::id).containsExactly(oldest.id());
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
    }

    @Test
    void renvoie_les_lignes_de_chaque_commande() {
        adapter.save(orderCreatedAt("2026-10-01T10:00:00Z"));

        Order found = adapter.findAll(OrderSearchCriteria.none(), new PageQuery(0, 20)).content().getFirst();

        assertThat(found.lines()).hasSize(1);
        assertThat(found.total()).isEqualTo(Money.of(new BigDecimal("12.50"), "EUR"));
    }

    // ─── Recherche et tri (étape 3) ───────────────────────────────────────────────

    private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);

    /** Une commande d'un client, avec des lignes « CODE:quantité:prix ». */
    private Order order(String customerId, String instant, String... lines) {
        List<OrderLine> orderLines = java.util.Arrays.stream(lines).map(spec -> {
            String[] parts = spec.split(":");
            return new OrderLine(parts[0], Integer.parseInt(parts[1]), Money.of(new BigDecimal(parts[2]), "EUR"));
        }).toList();
        return Order.create(customerId, orderLines, Instant.parse(instant));
    }

    private List<Order> search(String text, SortOrder sort) {
        return adapter.findAll(new OrderSearchCriteria(text, sort), FIRST_PAGE).content();
    }

    @Test
    void cherche_dans_l_identifiant_du_client_sans_tenir_compte_des_majuscules() {
        Order alice = adapter.save(order("ALICE-1", "2026-10-01T10:00:00Z", "BOOK:1:12.50"));
        adapter.save(order("BOB-1", "2026-10-02T10:00:00Z", "BOOK:1:12.50"));

        assertThat(search("alice", null)).extracting(Order::id).containsExactly(alice.id());
    }

    @Test
    void cherche_dans_le_code_des_produits_commandes() {
        Order withPen = adapter.save(order("C-001", "2026-10-01T10:00:00Z", "BOOK:1:12.50", "PEN:2:1.20"));
        adapter.save(order("C-002", "2026-10-02T10:00:00Z", "BOOK:1:12.50"));

        assertThat(search("pen", null)).extracting(Order::id).containsExactly(withPen.id());
    }

    @Test
    void compte_et_pagine_les_commandes_et_non_les_lignes_qui_correspondent() {
        // BOOK et NOTEBOOK contiennent tous les deux « book » : chaque commande a 2 lignes qui correspondent.
        // Avec une jointure, le SQL renverrait 4 lignes : le COUNT vaudrait 4, et LIMIT/OFFSET découperait
        // les lignes, pas les commandes (la même commande sur 2 pages, l'autre sur aucune).
        Order older = adapter.save(order("C-001", "2026-10-01T10:00:00Z", "BOOK:1:12.50", "NOTEBOOK:1:3.00"));
        Order newer = adapter.save(order("C-002", "2026-10-02T10:00:00Z", "BOOK:1:12.50", "NOTEBOOK:1:3.00"));

        PageResult<Order> first = adapter.findAll(new OrderSearchCriteria("book", null), new PageQuery(0, 1));
        PageResult<Order> second = adapter.findAll(new OrderSearchCriteria("book", null), new PageQuery(1, 1));

        assertThat(first.totalElements()).isEqualTo(2);
        assertThat(first.content()).extracting(Order::id).containsExactly(newer.id());
        assertThat(second.content()).extracting(Order::id).containsExactly(older.id());
    }

    @Test
    void echappe_les_caracteres_speciaux_de_like() {
        adapter.save(order("C-001", "2026-10-01T10:00:00Z", "BOOK:1:12.50"));

        // « % » ne doit pas être interprété comme « n'importe quoi »
        assertThat(search("%", null)).isEmpty();
    }

    @Test
    void trie_par_montant_croissant_ou_decroissant() {
        Order small = adapter.save(order("C-001", "2026-10-01T10:00:00Z", "PEN:1:1.20"));
        Order big = adapter.save(order("C-002", "2026-10-02T10:00:00Z", "BOOK:4:12.50"));
        Order medium = adapter.save(order("C-003", "2026-10-03T10:00:00Z", "BOOK:1:12.50"));

        assertThat(search(null, SortOrder.HIGHEST_TOTAL_FIRST)).extracting(Order::id)
                .containsExactly(big.id(), medium.id(), small.id());
        assertThat(search(null, SortOrder.LOWEST_TOTAL_FIRST)).extracting(Order::id)
                .containsExactly(small.id(), medium.id(), big.id());
    }

    @Test
    void trie_des_plus_anciennes_aux_plus_recentes() {
        Order newest = adapter.save(order("C-001", "2026-10-03T10:00:00Z", "BOOK:1:12.50"));
        Order oldest = adapter.save(order("C-002", "2026-10-01T10:00:00Z", "BOOK:1:12.50"));

        assertThat(search(null, SortOrder.OLDEST_FIRST)).extracting(Order::id)
                .containsExactly(oldest.id(), newest.id());
    }

    @Test
    void combine_la_recherche_et_le_tri() {
        Order smallPen = adapter.save(order("C-001", "2026-10-01T10:00:00Z", "PEN:1:1.20"));
        Order bigPen = adapter.save(order("C-002", "2026-10-02T10:00:00Z", "PEN:10:1.20", "BOOK:1:12.50"));
        adapter.save(order("C-003", "2026-10-03T10:00:00Z", "BOOK:9:12.50"));

        assertThat(search("pen", SortOrder.HIGHEST_TOTAL_FIRST)).extracting(Order::id)
                .containsExactly(bigPen.id(), smallPen.id());
    }
}