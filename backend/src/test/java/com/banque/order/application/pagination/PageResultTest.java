package com.banque.order.application.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Types de pagination : Java pur, testés sans mock. */
class PageResultTest {

    @Test
    void calcule_le_nombre_de_pages_en_arrondissant_au_superieur() {
        Assertions.assertThat(new PageResult<>(List.of(), 0, 20, 42).totalPages()).isEqualTo(3);
        assertThat(new PageResult<>(List.of(), 0, 20, 40).totalPages()).isEqualTo(2);
    }

    @Test
    void vaut_zero_page_quand_il_n_y_a_aucun_element() {
        // à toi : 0 élément → totalPages() vaut 0 (indice AssertJ : .isZero())
        assertThat(new PageResult<>(List.of(), 0, 20, 0).totalPages()).isZero();
    }

    @Test
    void map_transforme_le_contenu_sans_toucher_a_la_pagination() {
        // à toi : partir de new PageResult<>(List.of(1, 2), 1, 2, 5), faire .map(n -> "#" + n),
        // puis vérifier le contenu ("#1", "#2" → containsExactly) et que page et totalElements sont inchangés
        PageResult<String> page = new PageResult<>(List.of(1, 2), 1, 2, 5).map(n -> "#" + n);

        assertThat(page.content()).containsExactly("#1", "#2");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(5);
    }

    @Test
    void refuse_une_demande_de_page_invalide() {
        assertThatThrownBy(() -> new PageQuery(-1, 20)).isInstanceOf(IllegalArgumentException.class);
        // à toi : la même chose pour une taille de 0, et une taille de 101
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageQuery(0, 101)).isInstanceOf(IllegalArgumentException.class);
    }
}