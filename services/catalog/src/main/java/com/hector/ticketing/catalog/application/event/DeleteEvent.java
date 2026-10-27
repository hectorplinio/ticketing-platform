package com.hector.ticketing.catalog.application.event;

import com.hector.ticketing.catalog.domain.event.EventNotFoundException;
import com.hector.ticketing.catalog.domain.event.EventRepository;
import java.util.UUID;

public class DeleteEvent {

    private final EventRepository eventRepository;

    public DeleteEvent(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void execute(UUID id) {
        if (eventRepository.findById(id).isEmpty()) {
            throw new EventNotFoundException(id);
        }
        eventRepository.deleteById(id);
    }
}
