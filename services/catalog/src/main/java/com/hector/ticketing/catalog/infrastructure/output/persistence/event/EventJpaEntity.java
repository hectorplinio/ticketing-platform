package com.hector.ticketing.catalog.infrastructure.output.persistence.event;

import com.hector.ticketing.catalog.domain.event.Event;
import com.hector.ticketing.commons.persistence.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistence model of {@link Event}, mapped to the {@code event} table (V2 migration).
 *
 * <p>The venue is a plain {@code venue_id} column, not a {@code @ManyToOne}: Event and Venue are
 * separate aggregates, so one is never loaded through the other. The database FK keeps them
 * consistent.
 */
@Entity
@Table(name = "event")
public class EventJpaEntity extends BaseJpaEntity {

    @Column(name = "venue_id", nullable = false, updatable = false)
    private UUID venueId;

    @Column(name = "name", nullable = false, length = Event.NAME_MAX_LENGTH)
    private String name;

    @Column(name = "description", length = Event.DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    /** Required by JPA; not for application code. */
    protected EventJpaEntity() {}

    public EventJpaEntity(UUID id, UUID venueId, String name, String description, Instant startsAt, Instant endsAt) {
        super(id);
        this.venueId = venueId;
        this.name = name;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /** Copies the domain state onto an already persisted entity, keeping its id and created_at. */
    public void update(String name, String description, Instant startsAt, Instant endsAt) {
        this.name = name;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
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
}
