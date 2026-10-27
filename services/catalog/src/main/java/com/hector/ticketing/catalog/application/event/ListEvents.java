package com.hector.ticketing.catalog.application.event;

import com.hector.ticketing.catalog.domain.event.Event;
import com.hector.ticketing.catalog.domain.event.EventRepository;
import java.util.List;

public class ListEvents {

    private final EventRepository eventRepository;

    public ListEvents(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> execute() {
        return eventRepository.findAll();
    }
}
