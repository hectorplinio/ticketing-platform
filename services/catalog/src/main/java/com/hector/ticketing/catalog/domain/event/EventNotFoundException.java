package com.hector.ticketing.catalog.domain.event;

import com.hector.ticketing.commons.domain.NotFoundException;
import java.util.UUID;

public class EventNotFoundException extends NotFoundException {
    public EventNotFoundException(UUID id) {
        super("Event " + id + " not found");
    }
}
