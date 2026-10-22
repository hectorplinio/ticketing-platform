package com.hector.ticketing.catalog.application.venue;

import java.util.UUID;

public record UpdateVenueCommand(UUID id, String name, String address, int capacity) {}
