package com.banque.order.application.pagination;

/**
 * Une demande de page : numéro (à partir de 0) et taille.
 * Type à nous, sans Spring : le cœur ne dépend pas de Pageable.
 */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Le numéro de page doit être positif ou nul");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("La taille de page doit être comprise entre 1 et " + MAX_SIZE);
        }
    }
}