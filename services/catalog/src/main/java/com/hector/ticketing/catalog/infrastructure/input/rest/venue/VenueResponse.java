package com.hector.ticketing.catalog.infrastructure.input.rest.venue;

import com.hector.ticketing.catalog.domain.venue.Venue;
import java.util.UUID;

/** Response body: the API contract, decoupled from the domain model so either can change alone. */
record VenueResponse(UUID id, String name, String address, int capacity) {

    static VenueResponse from(Venue venue) {
        return new VenueResponse(venue.getId(), venue.getName(), venue.getAddress(), venue.getCapacity());
    }
}
