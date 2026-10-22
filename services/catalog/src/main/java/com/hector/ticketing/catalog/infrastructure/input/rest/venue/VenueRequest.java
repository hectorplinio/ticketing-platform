package com.hector.ticketing.catalog.infrastructure.input.rest.venue;

import com.hector.ticketing.catalog.application.venue.CreateVenueCommand;
import com.hector.ticketing.catalog.application.venue.UpdateVenueCommand;
import com.hector.ticketing.catalog.domain.venue.Venue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request body for creating and updating a venue. Bean Validation gives clients per-field errors;
 * the domain still enforces the same rules, so it is never left relying on this layer.
 */
record VenueRequest(
        @NotBlank @Size(max = Venue.NAME_MAX_LENGTH) String name,
        @NotBlank @Size(max = Venue.ADDRESS_MAX_LENGTH) String address,
        // Integer, not int: a missing field must be a 400, not silently 0
        @NotNull @Positive Integer capacity) {

    CreateVenueCommand toCreateCommand() {
        return new CreateVenueCommand(name, address, capacity);
    }

    UpdateVenueCommand toUpdateCommand(UUID id) {
        return new UpdateVenueCommand(id, name, address, capacity);
    }
}
