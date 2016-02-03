# --- !Ups

CREATE TABLE trusted_users (
    id    BIGINT NOT NULL PRIMARY KEY REFERENCES users(id)
);

# --- !Downs

DROP TABLE trusted_users;