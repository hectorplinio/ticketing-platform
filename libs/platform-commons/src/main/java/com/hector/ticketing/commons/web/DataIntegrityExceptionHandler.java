package com.hector.ticketing.commons.web;

import org.springframework.core.Ordered;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Maps database constraint violations (foreign key, unique, check) to {@code 409 Conflict}, e.g.
 * deleting a venue that still has events.
 *
 * <p>Separate from {@link GlobalExceptionHandler} because {@link DataIntegrityViolationException}
 * lives in spring-tx, which services without a database (notification) do not have: it is only
 * registered when that class is present. Spring asks controller advices in order and the first one
 * with a matching handler wins, so this one must run before the global catch-all for {@code
 * Exception}.
 */
@RestControllerAdvice
class DataIntegrityExceptionHandler implements Ordered {

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        // The database message (constraint name, values) is logged but never returned to the client.
        GlobalExceptionHandler.logClientError(HttpStatus.CONFLICT, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The request conflicts with the current state of the data.");
        problem.setTitle("Conflict");
        return problem;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
