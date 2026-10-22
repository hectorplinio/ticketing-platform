package com.hector.ticketing.catalog.domain.venue;

import com.hector.ticketing.commons.domain.DomainValidationException;
import java.util.UUID;

public class Venue {

    public static final int NAME_MAX_LENGTH = 150;
    public static final int ADDRESS_MAX_LENGTH = 300;

    private final UUID id;
    private String name;
    private String address;
    private int capacity;

    public Venue(UUID id, String name, String address, int capacity) {
        if (id == null) {
            throw new DomainValidationException("Venue id is required");
        }
        this.id = id;
        this.name = requireText(name, "name", NAME_MAX_LENGTH);
        this.address = requireText(address, "address", ADDRESS_MAX_LENGTH);
        this.capacity = requirePositive(capacity, "capacity");
    }

    public static Venue create(String name, String address, int capacity) {
        return new Venue(UUID.randomUUID(), name, address, capacity);
    }

    public void update(String name, String address, int capacity) {
        // validate everything before assigning, so a failed update leaves the venue unchanged
        String validName = requireText(name, "name", NAME_MAX_LENGTH);
        String validAddress = requireText(address, "address", ADDRESS_MAX_LENGTH);
        int validCapacity = requirePositive(capacity, "capacity");
        this.name = validName;
        this.address = validAddress;
        this.capacity = validCapacity;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getCapacity() {
        return capacity;
    }

    // Returns the value without leading/trailing whitespace, so "  Teatro Real " is stored as "Teatro Real".
    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Venue " + field + " is required");
        }
        String stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new DomainValidationException("Venue " + field + " must be at most " + maxLength + " characters");
        }
        return stripped;
    }

    private static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new DomainValidationException("Venue " + field + " must be greater than 0");
        }
        return value;
    }
}
