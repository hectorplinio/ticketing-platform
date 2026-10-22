package com.hector.ticketing.commons.web;

import com.hector.ticketing.commons.domain.DomainValidationException;
import com.hector.ticketing.commons.domain.NotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 Problem Details ({@code application/problem+json}). The base class
 * already covers Spring MVC errors (malformed JSON, invalid UUID in the path, unsupported method...).
 *
 * <p>Logging by severity: client errors (4xx) are logged as WARN in one line, server errors (5xx) as
 * ERROR with the stack trace. The response never contains internal details such as exception
 * messages of unexpected errors or stack traces.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex, WebRequest request) {
        logClientError(HttpStatus.NOT_FOUND, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        return problem;
    }

    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail handleDomainValidation(DomainValidationException ex, WebRequest request) {
        logClientError(HttpStatus.BAD_REQUEST, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    /**
     * Last resort for anything not handled above (bugs, infrastructure failures...). Spring picks the
     * most specific handler first, so this only runs when nothing else matches.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error on {}", request.getDescription(false), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal server error");
        return problem;
    }

    /** Adds an {@code errors} map (field -> message) to the default Bean Validation response. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> Objects.requireNonNullElse(error.getDefaultMessage(), "is invalid"),
                        (first, second) -> first,
                        LinkedHashMap::new));
        ex.getBody().setTitle("Invalid request");
        ex.getBody().setProperty("errors", errors);
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    /** Every Spring MVC error handled by the base class goes through here: log it by severity. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("Server error {} on {}", statusCode.value(), request.getDescription(false), ex);
        } else {
            logClientError(statusCode, ex, request);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    static void logClientError(HttpStatusCode status, Exception ex, WebRequest request) {
        log.warn("Client error {} on {}: {}", status.value(), request.getDescription(false), ex.getMessage());
    }
}
