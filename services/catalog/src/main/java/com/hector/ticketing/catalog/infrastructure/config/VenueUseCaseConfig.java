package com.hector.ticketing.catalog.infrastructure.config;

import com.hector.ticketing.catalog.application.venue.CreateVenue;
import com.hector.ticketing.catalog.application.venue.DeleteVenue;
import com.hector.ticketing.catalog.application.venue.GetVenue;
import com.hector.ticketing.catalog.application.venue.ListVenues;
import com.hector.ticketing.catalog.application.venue.UpdateVenue;
import com.hector.ticketing.catalog.domain.venue.VenueRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the venue use cases as Spring beans. Use cases are plain Java (no @Service), so the
 * application layer stays free of framework dependencies; the wiring lives here instead.
 */
@Configuration
class VenueUseCaseConfig {

    @Bean
    CreateVenue createVenue(VenueRepository venueRepository) {
        return new CreateVenue(venueRepository);
    }

    @Bean
    GetVenue getVenue(VenueRepository venueRepository) {
        return new GetVenue(venueRepository);
    }

    @Bean
    ListVenues listVenues(VenueRepository venueRepository) {
        return new ListVenues(venueRepository);
    }

    @Bean
    UpdateVenue updateVenue(VenueRepository venueRepository) {
        return new UpdateVenue(venueRepository);
    }

    @Bean
    DeleteVenue deleteVenue(VenueRepository venueRepository) {
        return new DeleteVenue(venueRepository);
    }
}
