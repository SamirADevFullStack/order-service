package com.banque.order.application.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Critères de recherche : Java pur, testés sans mock. */
class OrderSearchCriteriaTest {

    @Test
    void un_texte_absent_ou_vide_ne_filtre_rien() {
        assertThat(new OrderSearchCriteria(null, null).hasText()).isFalse();
        assertThat(new OrderSearchCriteria("", null).hasText()).isFalse();
        assertThat(new OrderSearchCriteria("   ", null).hasText()).isFalse();
    }

    @Test
    void retire_les_espaces_autour_du_texte() {
        assertThat(new OrderSearchCriteria("  pen ", null).text()).isEqualTo("pen");
    }

    @Test
    void trie_par_defaut_des_plus_recentes_aux_plus_anciennes() {
        assertThat(new OrderSearchCriteria("pen", null).sort()).isEqualTo(SortOrder.NEWEST_FIRST);
        assertThat(OrderSearchCriteria.none().sort()).isEqualTo(SortOrder.NEWEST_FIRST);
    }
}
