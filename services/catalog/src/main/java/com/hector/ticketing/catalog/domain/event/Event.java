package com.hector.ticketing.catalog.domain.event;

import com.hector.ticketing.commons.domain.DomainValidationException;
import java.time.Instant;
import java.util.UUID;

public class Event {

    public static final int NAME_MAX_LENGTH = 150;
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    private final UUID id;
    // final: an event never moves to another venue; to change it, delete the event and create a new one
    private final UUID venueId;
    private String name;
    private String description; // optional: null when there is none
    private Instant startsAt;
    private Instant endsAt;

    public Event(UUID id, UUID venueId, String name, String description, Instant startsAt, Instant endsAt) {
        if (id == null) {
            throw new DomainValidationException("Event id is required");
        }
        if (venueId == null) {
            throw new DomainValidationException("Event venueId is required");
        }
        requireValidDates(startsAt, endsAt);
        this.id = id;
        this.venueId = venueId;
        this.name = requireText(name, "name", NAME_MAX_LENGTH);
        this.description = optionalText(description, "description", DESCRIPTION_MAX_LENGTH);
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public static Event create(UUID venueId, String name, String description, Instant startsAt, Instant endsAt) {
        return new Event(UUID.randomUUID(), venueId, name, description, startsAt, endsAt);
    }

    public void update(String name, String description, Instant startsAt, Instant endsAt) {
        // validate everything before assigning, so a failed update leaves the event unchanged
        String validName = requireText(name, "name", NAME_MAX_LENGTH);
        String validDescription = optionalText(description, "description", DESCRIPTION_MAX_LENGTH);
        requireValidDates(startsAt, endsAt);
        this.name = validName;
        this.description = validDescription;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVenueId() {
        return venueId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    // Returns the value without leading/trailing whitespace, so "  Rock Fest " is stored as "Rock Fest".
    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Event " + field + " is required");
        }
        return stripWithinMaxLength(value, field, maxLength);
    }

    // Blank means "no value": stored as null, so there is a single way to say "no description".
    private static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return stripWithinMaxLength(value, field, maxLength);
    }

    private static String stripWithinMaxLength(String value, String field, int maxLength) {
        String stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new DomainValidationException("Event " + field + " must be at most " + maxLength + " characters");
        }
        return stripped;
    }

    private static void requireValidDates(Instant startsAt, Instant endsAt) {
        if (startsAt == null || endsAt == null) {
            throw new DomainValidationException("Event start and end dates are required");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new DomainValidationException("Event end must be after its start");
        }
    }
}
