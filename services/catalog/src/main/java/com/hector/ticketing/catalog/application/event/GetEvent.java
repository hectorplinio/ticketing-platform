package com.hector.ticketing.catalog.application.event;

import com.hector.ticketing.catalog.domain.event.Event;
import com.hector.ticketing.catalog.domain.event.EventNotFoundException;
import com.hector.ticketing.catalog.domain.event.EventRepository;
import java.util.UUID;

public class GetEvent {

    private final EventRepository eventRepository;

    public GetEvent(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Event execute(UUID id) {
        return eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }
}
