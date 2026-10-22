package com.hector.ticketing.catalog.application.venue;

import com.hector.ticketing.catalog.domain.venue.Venue;
import com.hector.ticketing.catalog.domain.venue.VenueRepository;

public class CreateVenue {

    private final VenueRepository venueRepository;

    public CreateVenue(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public Venue execute(CreateVenueCommand command) {
        Venue venue = Venue.create(command.name(), command.address(), command.capacity());
        venueRepository.save(venue);
        return venue;
    }
}
