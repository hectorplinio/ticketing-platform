package com.hector.ticketing.catalog.application.venue;

import com.hector.ticketing.catalog.domain.venue.Venue;
import com.hector.ticketing.catalog.domain.venue.VenueNotFoundException;
import com.hector.ticketing.catalog.domain.venue.VenueRepository;
import java.util.UUID;

public class GetVenue {

    private final VenueRepository venueRepository;

    public GetVenue(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public Venue execute(UUID id) {
        return venueRepository.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
    }
}
