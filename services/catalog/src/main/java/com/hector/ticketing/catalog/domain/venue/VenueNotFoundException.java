package com.hector.ticketing.catalog.domain.venue;

import com.hector.ticketing.catalog.domain.shared.NotFoundException;
import java.util.UUID;

public class VenueNotFoundException extends NotFoundException {

    public VenueNotFoundException(UUID id) {
        super("Venue " + id + " not found");
    }
}
