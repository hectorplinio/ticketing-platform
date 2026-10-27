package com.hector.ticketing.catalog.application.event;

import com.hector.ticketing.catalog.domain.event.Event;
import com.hector.ticketing.catalog.domain.event.EventRepository;
import com.hector.ticketing.catalog.domain.venue.VenueNotFoundException;
import com.hector.ticketing.catalog.domain.venue.VenueRepository;

public class CreateEvent {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;

    public CreateEvent(EventRepository eventRepository, VenueRepository venueRepository) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
    }

    public Event execute(CreateEventCommand command) {
        // The FK would reject an unknown venue too, but as a generic 409; checking here gives a clear 404
        if (venueRepository.findById(command.venueId()).isEmpty()) {
            throw new VenueNotFoundException(command.venueId());
        }
        Event event = Event.create(
                command.venueId(), command.name(), command.description(), command.startsAt(), command.endsAt());
        eventRepository.save(event);
        return event;
    }
}
