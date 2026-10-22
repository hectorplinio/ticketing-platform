package com.hector.ticketing.catalog.infrastructure.output.persistence.venue;

import com.hector.ticketing.catalog.domain.venue.Venue;
import com.hector.ticketing.catalog.infrastructure.output.persistence.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Persistence model of {@link Venue}, mapped to the {@code venue} table (V1 migration).
 *
 * <p>Separate from the domain model on purpose: it carries JPA annotations and technical columns,
 * so the domain stays pure Java. The adapter maps between both. Validation lives in the domain;
 * this class just stores what it is given.
 */
@Entity
@Table(name = "venue")
public class VenueJpaEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false, length = Venue.NAME_MAX_LENGTH)
    private String name;

    @Column(name = "address", nullable = false, length = Venue.ADDRESS_MAX_LENGTH)
    private String address;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    /** Required by JPA; not for application code. */
    protected VenueJpaEntity() {}

    public VenueJpaEntity(UUID id, String name, String address, int capacity) {
        super(id);
        this.name = name;
        this.address = address;
        this.capacity = capacity;
    }

    /** Copies the domain state onto an already persisted entity, keeping its id and created_at. */
    public void update(String name, String address, int capacity) {
        this.name = name;
        this.address = address;
        this.capacity = capacity;
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
}
