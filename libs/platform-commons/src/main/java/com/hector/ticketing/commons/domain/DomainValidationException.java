package com.hector.ticketing.commons.domain;

/** A domain rule was violated by the given input (e.g. a blank name). Maps to HTTP 400. */
public class DomainValidationException extends RuntimeException {

    public DomainValidationException(String message) {
        super(message);
    }
}
