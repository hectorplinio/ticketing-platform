package com.hector.ticketing.catalog.infrastructure.config;

import com.hector.ticketing.catalog.application.event.CreateEvent;
import com.hector.ticketing.catalog.application.event.DeleteEvent;
import com.hector.ticketing.catalog.application.event.GetEvent;
import com.hector.ticketing.catalog.application.event.ListEvents;
import com.hector.ticketing.catalog.application.event.UpdateEvent;
import com.hector.ticketing.catalog.domain.event.EventRepository;
import com.hector.ticketing.catalog.domain.venue.VenueRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the event use cases as Spring beans (see {@link VenueUseCaseConfig} for why). */
@Configuration
class EventUseCaseConfig {

    @Bean
    CreateEvent createEvent(EventRepository eventRepository, VenueRepository venueRepository) {
        return new CreateEvent(eventRepository, venueRepository);
    }

    @Bean
    GetEvent getEvent(EventRepository eventRepository) {
        return new GetEvent(eventRepository);
    }

    @Bean
    ListEvents listEvents(EventRepository eventRepository) {
        return new ListEvents(eventRepository);
    }

    @Bean
    UpdateEvent updateEvent(EventRepository eventRepository) {
        return new UpdateEvent(eventRepository);
    }

    @Bean
    DeleteEvent deleteEvent(EventRepository eventRepository) {
        return new DeleteEvent(eventRepository);
    }
}
