package com.hector.ticketing.catalog.application.venue;

import com.hector.ticketing.catalog.domain.venue.VenueNotFoundException;
import com.hector.ticketing.catalog.domain.venue.VenueRepository;
import java.util.UUID;

public class DeleteVenue {

    private final VenueRepository venueRepository;

    public DeleteVenue(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public void execute(UUID id) {
        if (venueRepository.findById(id).isEmpty()) {
            throw new VenueNotFoundException(id);
        }
        venueRepository.deleteById(id);
    }
}
