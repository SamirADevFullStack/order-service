package com.banque.order.infrastructure.in.rest;

import com.banque.order.domain.exception.InvalidOrderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions du domaine en réponses HTTP (format standard ProblemDetail, RFC 7807).
 * Le domaine lève une exception métier ; c'est l'adaptateur REST qui décide que ça vaut un 400.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(InvalidOrderException.class)
    public ProblemDetail handleInvalidOrder(InvalidOrderException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Commande invalide");
        return problem;
    }
}
