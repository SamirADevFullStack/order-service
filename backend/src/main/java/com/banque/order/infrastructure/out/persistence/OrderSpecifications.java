package com.banque.order.infrastructure.out.persistence;

import com.banque.order.application.search.OrderSearchCriteria;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Traduit les critères de recherche (à nous) en Specification JPA (une clause WHERE construite dynamiquement).
 * Un critère absent n'ajoute aucune condition.
 */
final class OrderSpecifications {

    private static final char ESCAPE = '\\';

    private OrderSpecifications() {
    }

    static Specification<OrderJpaEntity> matching(OrderSearchCriteria criteria) {
        if (!criteria.hasText()) {
            return (root, query, cb) -> cb.conjunction();   // WHERE 1=1 : aucune restriction
        }
        String pattern = containsPattern(criteria.text());

        return (root, query, cb) -> {
            // 1. le client contient le texte
            var customerMatches = cb.like(cb.lower(root.get("customerId")), pattern, ESCAPE);

            // 2. OU au moins une ligne a un code produit qui contient le texte.
            //    EXISTS (sous-requête) plutôt qu'une jointure : une commande avec 2 lignes correspondantes
            //    sortirait 2 fois avec un JOIN, et le COUNT de la pagination serait faux.
            Subquery<UUID> lineMatches = query.subquery(UUID.class);
            Root<OrderJpaEntity> order = lineMatches.from(OrderJpaEntity.class);
            Join<OrderJpaEntity, OrderLineJpaEntity> line = order.join("lines");
            lineMatches.select(order.get("id")).where(
                    cb.equal(order.get("id"), root.get("id")),
                    cb.like(cb.lower(line.get("productCode")), pattern, ESCAPE));

            return cb.or(customerMatches, cb.exists(lineMatches));
        };
    }

    /**
     * « pen » → « %pen% », en minuscules. Les caractères spéciaux de LIKE (%, _) saisis par l'utilisateur
     * sont échappés : chercher « 100% » ne doit pas tout renvoyer.
     */
    private static String containsPattern(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace(String.valueOf(ESCAPE), ESCAPE + "" + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
        return "%" + escaped + "%";
    }
}
