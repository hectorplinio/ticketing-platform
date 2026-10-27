package com.hector.ticketing.catalog.domain.event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {

    void save(Event event);

    Optional<Event> findById(UUID id);

    List<Event> findAll();

    void deleteById(UUID id);
}
