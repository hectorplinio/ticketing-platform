package com.hector.ticketing.catalog.domain.venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueRepository {

    void save(Venue venue);

    Optional<Venue> findById(UUID id);

    List<Venue> findAll();

    void deleteById(UUID id);
}
