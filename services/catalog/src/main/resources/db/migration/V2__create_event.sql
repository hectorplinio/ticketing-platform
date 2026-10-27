-- Something that happens at a venue on a given date. Mapped by EventJpaEntity.
-- Lengths match Event.NAME_MAX_LENGTH / DESCRIPTION_MAX_LENGTH, so the domain rejects bad input first.
CREATE TABLE event
(
    id          uuid          NOT NULL,
    venue_id    uuid          NOT NULL,
    name        varchar(150)  NOT NULL,
    description varchar(2000),
    starts_at   timestamptz   NOT NULL,
    ends_at     timestamptz   NOT NULL,
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT pk_event PRIMARY KEY (id),
    -- No ON DELETE CASCADE: deleting a venue that still has events fails (409), it never wipes them
    CONSTRAINT fk_event_venue FOREIGN KEY (venue_id) REFERENCES venue (id),
    CONSTRAINT ck_event_dates CHECK (ends_at > starts_at)
);

-- Postgres does not index foreign keys automatically; needed for lookups by venue and for the FK check on venue delete
CREATE INDEX ix_event_venue_id ON event (venue_id);
