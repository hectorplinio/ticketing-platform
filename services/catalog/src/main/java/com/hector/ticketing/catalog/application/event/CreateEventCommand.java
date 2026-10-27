package com.hector.ticketing.catalog.application.event;

import java.time.Instant;
import java.util.UUID;

public record CreateEventCommand(UUID venueId, String name, String description, Instant startsAt, Instant endsAt) {}
