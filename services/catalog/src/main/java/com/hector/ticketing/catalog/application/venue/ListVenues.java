package com.hector.ticketing.catalog.application.venue;

import com.hector.ticketing.catalog.domain.venue.Venue;
import com.hector.ticketing.catalog.domain.venue.VenueRepository;
import java.util.List;

public class ListVenues {

    private final VenueRepository venueRepository;

    public ListVenues(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public List<Venue> execute() {
        return venueRepository.findAll();
    }
}
