package com.hector.ticketing.catalog.infrastructure.output.persistence.venue;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data generates the implementation. Package-private: only the adapter may use it. */
interface VenueJpaRepository extends JpaRepository<VenueJpaEntity, UUID> {}
