# --- !Ups

CREATE TABLE trusted_users (
    id    BIGINT NOT NULL PRIMARY KEY
);

# --- !Downs

DROP TABLE trusted_users;