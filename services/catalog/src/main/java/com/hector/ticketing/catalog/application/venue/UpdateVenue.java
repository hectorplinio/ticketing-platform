package com.hector.ticketing.catalog.application.venue;

import com.hector.ticketing.catalog.domain.venue.Venue;
import com.hector.ticketing.catalog.domain.venue.VenueNotFoundException;
import com.hector.ticketing.catalog.domain.venue.VenueRepository;

public class UpdateVenue {

    private final VenueRepository venueRepository;

    public UpdateVenue(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public Venue execute(UpdateVenueCommand command) {
        Venue venue =
                venueRepository.findById(command.id()).orElseThrow(() -> new VenueNotFoundException(command.id()));
        venue.update(command.name(), command.address(), command.capacity());
        venueRepository.save(venue);
        return venue;
    }
}
