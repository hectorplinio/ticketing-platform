package com.hector.ticketing.catalog.application.event;

import java.time.Instant;
import java.util.UUID;

/** No venueId: an event never moves to another venue (see {@code Event}). */
public record UpdateEventCommand(UUID id, String name, String description, Instant startsAt, Instant endsAt) {}
