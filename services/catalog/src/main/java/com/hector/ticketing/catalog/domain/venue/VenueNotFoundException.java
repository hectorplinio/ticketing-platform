package com.hector.ticketing.catalog.domain.venue;

import com.hector.ticketing.commons.domain.NotFoundException;
import java.util.UUID;

public class VenueNotFoundException extends NotFoundException {

    public VenueNotFoundException(UUID id) {
        super("Venue " + id + " not found");
    }
}
