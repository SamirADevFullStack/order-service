package com.banque.order.application.pagination;

import java.util.List;
import java.util.function.Function;

/** Une page de résultats, et de quoi construire la navigation (nombre total d'éléments). */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        content = List.copyOf(content);   // copie défensive : la liste ne pourra plus être modifiée
    }

    /** Nombre total de pages : 42 éléments par pages de 20 → 3 pages. */
    public int totalPages() {
        // à toi : arrondir AU SUPÉRIEUR, et 0 élément → 0 page
        return (int) Math.ceil((double) totalElements / size);
    }

    /** Transforme le contenu en gardant les informations de pagination (domaine → DTO, par exemple). */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}