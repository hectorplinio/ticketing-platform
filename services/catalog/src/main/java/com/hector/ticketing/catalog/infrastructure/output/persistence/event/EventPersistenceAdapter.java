package com.hector.ticketing.catalog.infrastructure.output.persistence.event;

import com.hector.ticketing.catalog.domain.event.Event;
import com.hector.ticketing.catalog.domain.event.EventRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Output adapter: implements the domain port {@link EventRepository} with JPA, translating between
 * the domain model {@link Event} and the persistence model {@link EventJpaEntity}.
 */
@Component
class EventPersistenceAdapter implements EventRepository {

    private final EventJpaRepository jpaRepository;

    EventPersistenceAdapter(EventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(Event event) {
        // Update the loaded entity instead of saving a new instance, so id and created_at are kept.
        // Changes are flushed on commit by Hibernate's dirty checking.
        jpaRepository
                .findById(event.getId())
                .ifPresentOrElse(
                        entity -> entity.update(
                                event.getName(), event.getDescription(), event.getStartsAt(), event.getEndsAt()),
                        () -> jpaRepository.save(toEntity(event)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Event> findById(UUID id) {
        return jpaRepository.findById(id).map(EventPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> findAll() {
        return jpaRepository.findAll().stream()
                .map(EventPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private static EventJpaEntity toEntity(Event event) {
        return new EventJpaEntity(
                event.getId(),
                event.getVenueId(),
                event.getName(),
                event.getDescription(),
                event.getStartsAt(),
                event.getEndsAt());
    }

    private static Event toDomain(EventJpaEntity entity) {
        return new Event(
                entity.getId(),
                entity.getVenueId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStartsAt(),
                entity.getEndsAt());
    }
}
