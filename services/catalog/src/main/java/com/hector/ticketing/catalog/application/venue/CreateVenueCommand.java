package com.hector.ticketing.catalog.application.venue;

public record CreateVenueCommand(String name, String address, int capacity) {}
