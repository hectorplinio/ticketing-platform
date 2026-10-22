package com.hector.ticketing.commons.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

/**
 * Columns shared by every table: {@code id}, {@code created_at} and {@code updated_at}.
 *
 * <p>{@link MappedSuperclass} contributes these fields to each subclass's own table; it has no
 * table of its own. Every migration must still declare the three columns.
 *
 * <p>The id is assigned by the domain (e.g. {@code Venue.create()}), never generated here.
 * Timestamps are set by Hibernate and are never written from the domain.
 */
@MappedSuperclass
public abstract class BaseJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Since ids are assigned up front, Spring Data cannot tell new from existing by a null id and
    // would SELECT before every INSERT. Persistable lets us say it explicitly.
    @Transient
    private boolean isNew = true;

    /** Required by JPA; not for application code. */
    protected BaseJpaEntity() {}

    protected BaseJpaEntity(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity id is required");
        }
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Identity is the id, assigned before persisting, so it is stable for the object's whole life.
    // Hibernate.getClass unwraps lazy-loading proxies (subclasses of the real entity), and getId()
    // is used on both sides because an uninitialized proxy's fields are null.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BaseJpaEntity that) || Hibernate.getClass(this) != Hibernate.getClass(that)) {
            return false;
        }
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
