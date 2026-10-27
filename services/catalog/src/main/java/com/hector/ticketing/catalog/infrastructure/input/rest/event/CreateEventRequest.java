package com.hector.ticketing.catalog.infrastructure.input.rest.event;

import com.hector.ticketing.catalog.application.event.CreateEventCommand;
import com.hector.ticketing.catalog.domain.event.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Request body for creating an event. Bean Validation gives clients per-field errors; the domain
 * still enforces the same rules (and the date order, which spans two fields).
 */
record CreateEventRequest(
        @NotNull UUID venueId,
        @NotBlank @Size(max = Event.NAME_MAX_LENGTH) String name,
        @Size(max = Event.DESCRIPTION_MAX_LENGTH) String description,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {

    CreateEventCommand toCommand() {
        return new CreateEventCommand(venueId, name, description, startsAt, endsAt);
    }
}
