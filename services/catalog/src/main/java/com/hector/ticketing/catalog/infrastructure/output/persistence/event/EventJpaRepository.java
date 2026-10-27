package com.hector.ticketing.catalog.infrastructure.output.persistence.event;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data generates the implementation. Package-private: only the adapter may use it. */
interface EventJpaRepository extends JpaRepository<EventJpaEntity, UUID> {}
