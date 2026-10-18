-- Hạ tầng chung: extension cho exclusion constraint, ShedLock, Spring Modulith Event Publication Registry

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ShedLock (JDBC provider)
CREATE TABLE shedlock
(
  name       VARCHAR(64)  NOT NULL,
  lock_until TIMESTAMPTZ  NOT NULL,
  locked_at  TIMESTAMPTZ  NOT NULL,
  locked_by  VARCHAR(255) NOT NULL,
  PRIMARY KEY (name)
);

-- Spring Modulith 2.1 JDBC event publication (schema v2, schema-postgresql.sql của spring-modulith-events-jdbc)
CREATE TABLE event_publication
(
  id                     UUID                     NOT NULL,
  listener_id            TEXT                     NOT NULL,
  event_type             TEXT                     NOT NULL,
  serialized_event       TEXT                     NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
