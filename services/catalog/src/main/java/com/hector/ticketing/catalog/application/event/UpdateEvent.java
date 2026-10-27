package com.hector.ticketing.catalog.application.event;

import com.hector.ticketing.catalog.domain.event.Event;
import com.hector.ticketing.catalog.domain.event.EventNotFoundException;
import com.hector.ticketing.catalog.domain.event.EventRepository;

public class UpdateEvent {

    private final EventRepository eventRepository;

    public UpdateEvent(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Event execute(UpdateEventCommand command) {
        Event event =
                eventRepository.findById(command.id()).orElseThrow(() -> new EventNotFoundException(command.id()));
        event.update(command.name(), command.description(), command.startsAt(), command.endsAt());
        eventRepository.save(event);
        return event;
    }
}
