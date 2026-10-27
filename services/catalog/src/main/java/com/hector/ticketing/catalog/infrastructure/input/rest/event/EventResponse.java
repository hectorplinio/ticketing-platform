package com.hector.ticketing.catalog.infrastructure.input.rest.event;

import com.hector.ticketing.catalog.domain.event.Event;
import java.time.Instant;
import java.util.UUID;

/** Response body: the API contract, decoupled from the domain model so either can change alone. */
record EventResponse(UUID id, UUID venueId, String name, String description, Instant startsAt, Instant endsAt) {

    static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getVenueId(),
                event.getName(),
                event.getDescription(),
                event.getStartsAt(),
                event.getEndsAt());
    }
}
