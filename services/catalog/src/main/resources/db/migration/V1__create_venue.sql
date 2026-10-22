-- Physical place where events take place. Mapped by VenueJpaEntity.
-- id, created_at and updated_at are the common columns of every table (BaseJpaEntity).
-- Lengths match Venue.NAME_MAX_LENGTH / ADDRESS_MAX_LENGTH, so the domain rejects bad input first.
CREATE TABLE venue
(
    id         uuid         NOT NULL,
    name       varchar(150) NOT NULL,
    address    varchar(300) NOT NULL,
    capacity   integer      NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_venue PRIMARY KEY (id),
    CONSTRAINT ck_venue_capacity_positive CHECK (capacity > 0)
);
