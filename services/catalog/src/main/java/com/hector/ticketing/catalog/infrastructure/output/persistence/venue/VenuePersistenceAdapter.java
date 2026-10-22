package com.hector.ticketing.catalog.infrastructure.output.persistence.venue;

import com.hector.ticketing.catalog.domain.venue.Venue;
import com.hector.ticketing.catalog.domain.venue.VenueRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Output adapter: implements the domain port {@link VenueRepository} with JPA, translating between
 * the domain model {@link Venue} and the persistence model {@link VenueJpaEntity}.
 */
@Component
class VenuePersistenceAdapter implements VenueRepository {

    private final VenueJpaRepository jpaRepository;

    VenuePersistenceAdapter(VenueJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(Venue venue) {
        // Update the loaded entity instead of saving a new instance, so id and created_at are kept.
        // Changes are flushed on commit by Hibernate's dirty checking.
        jpaRepository
                .findById(venue.getId())
                .ifPresentOrElse(
                        entity -> entity.update(venue.getName(), venue.getAddress(), venue.getCapacity()),
                        () -> jpaRepository.save(toEntity(venue)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Venue> findById(UUID id) {
        return jpaRepository.findById(id).map(VenuePersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venue> findAll() {
        return jpaRepository.findAll().stream()
                .map(VenuePersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private static VenueJpaEntity toEntity(Venue venue) {
        return new VenueJpaEntity(venue.getId(), venue.getName(), venue.getAddress(), venue.getCapacity());
    }

    private static Venue toDomain(VenueJpaEntity entity) {
        return new Venue(entity.getId(), entity.getName(), entity.getAddress(), entity.getCapacity());
    }
}
