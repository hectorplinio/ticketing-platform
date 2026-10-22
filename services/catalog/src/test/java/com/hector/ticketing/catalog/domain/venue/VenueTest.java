package com.hector.ticketing.catalog.domain.venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hector.ticketing.commons.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Pure unit tests: no Spring, no database. The domain rules are tested in isolation. */
class VenueTest {

    @Test
    void createGeneratesIdAndKeepsValues() {
        Venue venue = Venue.create("WiZink Center", "Av. Felipe II, Madrid", 15000);

        assertThat(venue.getId()).isNotNull();
        assertThat(venue.getName()).isEqualTo("WiZink Center");
        assertThat(venue.getAddress()).isEqualTo("Av. Felipe II, Madrid");
        assertThat(venue.getCapacity()).isEqualTo(15000);
    }

    @Test
    void createGeneratesDifferentIds() {
        assertThat(Venue.create("A", "Address", 1).getId())
                .isNotEqualTo(Venue.create("A", "Address", 1).getId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> Venue.create(name, "Address", 100))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Venue name is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankAddress(String address) {
        assertThatThrownBy(() -> Venue.create("Name", address, 100))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Venue address is required");
    }

    @Test
    void createStripsSurroundingWhitespace() {
        Venue venue = Venue.create("  WiZink Center ", "\tAv. Felipe II, Madrid  ", 15000);

        assertThat(venue.getName()).isEqualTo("WiZink Center");
        assertThat(venue.getAddress()).isEqualTo("Av. Felipe II, Madrid");
    }

    @Test
    void updateStripsSurroundingWhitespace() {
        Venue venue = Venue.create("Old name", "Old address", 100);

        venue.update(" New name ", " New address ", 200);

        assertThat(venue.getName()).isEqualTo("New name");
        assertThat(venue.getAddress()).isEqualTo("New address");
    }

    @Test
    void acceptsNameAtMaxLength() {
        String name = "a".repeat(Venue.NAME_MAX_LENGTH);

        assertThat(Venue.create(name, "Address", 100).getName()).isEqualTo(name);
    }

    @Test
    void rejectsNameOverMaxLength() {
        String name = "a".repeat(Venue.NAME_MAX_LENGTH + 1);

        assertThatThrownBy(() -> Venue.create(name, "Address", 100))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Venue name must be at most 150 characters");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonPositiveCapacity(int capacity) {
        assertThatThrownBy(() -> Venue.create("Name", "Address", capacity))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Venue capacity must be greater than 0");
    }

    @Test
    void updateChangesValuesAndKeepsId() {
        Venue venue = Venue.create("Old name", "Old address", 100);

        venue.update("New name", "New address", 200);

        assertThat(venue.getName()).isEqualTo("New name");
        assertThat(venue.getAddress()).isEqualTo("New address");
        assertThat(venue.getCapacity()).isEqualTo(200);
    }

    @Test
    void failedUpdateLeavesVenueUnchanged() {
        Venue venue = Venue.create("Old name", "Old address", 100);

        assertThatThrownBy(() -> venue.update("New name", "New address", -5))
                .isInstanceOf(DomainValidationException.class);

        assertThat(venue.getName()).isEqualTo("Old name");
        assertThat(venue.getAddress()).isEqualTo("Old address");
        assertThat(venue.getCapacity()).isEqualTo(100);
    }
}
