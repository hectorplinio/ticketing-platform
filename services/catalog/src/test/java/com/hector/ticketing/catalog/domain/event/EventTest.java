package com.hector.ticketing.catalog.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hector.ticketing.commons.domain.DomainValidationException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Pure unit tests: no Spring, no database. The domain rules are tested in isolation. */
class EventTest {

    // Fixed dates (not Instant.now()) so the tests give the same result every run
    private static final UUID VENUE_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-12-01T20:00:00Z");
    private static final Instant END = START.plus(Duration.ofHours(2));

    @Test
    void createGeneratesIdAndKeepsValues() {
        Event event = Event.create(VENUE_ID, "Rock Fest", "Open air concert", START, END);

        assertThat(event.getId()).isNotNull();
        assertThat(event.getVenueId()).isEqualTo(VENUE_ID);
        assertThat(event.getName()).isEqualTo("Rock Fest");
        assertThat(event.getDescription()).isEqualTo("Open air concert");
        assertThat(event.getStartsAt()).isEqualTo(START);
        assertThat(event.getEndsAt()).isEqualTo(END);
    }

    @Test
    void createGeneratesDifferentIds() {
        assertThat(Event.create(VENUE_ID, "A", null, START, END).getId())
                .isNotEqualTo(Event.create(VENUE_ID, "A", null, START, END).getId());
    }

    @Test
    void rejectsMissingVenueId() {
        assertThatThrownBy(() -> Event.create(null, "Rock Fest", null, START, END))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Event venueId is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> Event.create(VENUE_ID, name, null, START, END))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Event name is required");
    }

    @Test
    void acceptsNameAtMaxLength() {
        String name = "a".repeat(Event.NAME_MAX_LENGTH);

        assertThat(Event.create(VENUE_ID, name, null, START, END).getName()).isEqualTo(name);
    }

    @Test
    void rejectsNameOverMaxLength() {
        String name = "a".repeat(Event.NAME_MAX_LENGTH + 1);

        assertThatThrownBy(() -> Event.create(VENUE_ID, name, null, START, END))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Event name must be at most 150 characters");
    }

    @Test
    void createStripsSurroundingWhitespace() {
        Event event = Event.create(VENUE_ID, "  Rock Fest ", "\tOpen air concert  ", START, END);

        assertThat(event.getName()).isEqualTo("Rock Fest");
        assertThat(event.getDescription()).isEqualTo("Open air concert");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankDescriptionBecomesNull(String description) {
        assertThat(Event.create(VENUE_ID, "Rock Fest", description, START, END).getDescription())
                .isNull();
    }

    @Test
    void rejectsDescriptionOverMaxLength() {
        String description = "a".repeat(Event.DESCRIPTION_MAX_LENGTH + 1);

        assertThatThrownBy(() -> Event.create(VENUE_ID, "Rock Fest", description, START, END))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Event description must be at most 2000 characters");
    }

    @Test
    void rejectsMissingDates() {
        assertThatThrownBy(() -> Event.create(VENUE_ID, "Rock Fest", null, null, END))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Event start and end dates are required");
        assertThatThrownBy(() -> Event.create(VENUE_ID, "Rock Fest", null, START, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Event start and end dates are required");
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> Event.create(VENUE_ID, "Rock Fest", null, END, START))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Event end must be after its start");
    }

    @Test
    void rejectsEndEqualToStart() {
        assertThatThrownBy(() -> Event.create(VENUE_ID, "Rock Fest", null, START, START))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Event end must be after its start");
    }

    @Test
    void updateChangesValuesAndKeepsIdAndVenue() {
        Event event = Event.create(VENUE_ID, "Old name", "Old description", START, END);
        UUID id = event.getId();
        Instant newStart = START.plus(Duration.ofDays(1));
        Instant newEnd = newStart.plus(Duration.ofHours(3));

        event.update(" New name ", "New description", newStart, newEnd);

        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getVenueId()).isEqualTo(VENUE_ID);
        assertThat(event.getName()).isEqualTo("New name");
        assertThat(event.getDescription()).isEqualTo("New description");
        assertThat(event.getStartsAt()).isEqualTo(newStart);
        assertThat(event.getEndsAt()).isEqualTo(newEnd);
    }

    @Test
    void failedUpdateLeavesEventUnchanged() {
        Event event = Event.create(VENUE_ID, "Old name", "Old description", START, END);

        // valid name and description, invalid dates: nothing may be assigned
        assertThatThrownBy(() -> event.update("New name", "New description", END, START))
                .isInstanceOf(DomainValidationException.class);

        assertThat(event.getName()).isEqualTo("Old name");
        assertThat(event.getDescription()).isEqualTo("Old description");
        assertThat(event.getStartsAt()).isEqualTo(START);
        assertThat(event.getEndsAt()).isEqualTo(END);
    }
}
