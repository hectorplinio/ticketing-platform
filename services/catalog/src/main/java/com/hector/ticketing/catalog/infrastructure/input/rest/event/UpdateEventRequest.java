package com.hector.ticketing.catalog.infrastructure.input.rest.event;

import com.hector.ticketing.catalog.application.event.UpdateEventCommand;
import com.hector.ticketing.catalog.domain.event.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Request body for updating an event. Separate from {@link CreateEventRequest} because the venue
 * cannot change: a shared record would accept a venueId on PUT and silently ignore it.
 */
record UpdateEventRequest(
        @NotBlank @Size(max = Event.NAME_MAX_LENGTH) String name,
        @Size(max = Event.DESCRIPTION_MAX_LENGTH) String description,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {

    UpdateEventCommand toCommand(UUID id) {
        return new UpdateEventCommand(id, name, description, startsAt, endsAt);
    }
}
