package com.hector.ticketing.commons.domain;

/** Base for "aggregate X with id Y does not exist". Each aggregate extends it. Maps to HTTP 404. */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
