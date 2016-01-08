# --- !Ups

CREATE TABLE events (
  id         BIGSERIAL PRIMARY KEY,
  name       TEXT,
  start_date BIGINT,
  end_date   BIGINT,
  "offset"   BIGINT,
  location   TEXT,
  url        TEXT
);

CREATE TABLE users (
  id    BIGINT NOT NULL PRIMARY KEY,
  login TEXT
);

CREATE TABLE event_flags (
  event_id BIGINT NOT NULL REFERENCES events (id),
  user_id  BIGINT NOT NULL REFERENCES users (id),
  PRIMARY KEY (event_id, user_id)
);

CREATE TABLE event_joins (
  event_id BIGINT REFERENCES events (id),
  user_id  BIGINT REFERENCES users (id),
  PRIMARY KEY (event_id, user_id)
);

CREATE TABLE tokens (
  user_id BIGINT NOT NULL PRIMARY KEY REFERENCES users (id),
  token   TEXT
);

CREATE TABLE user_friends (
  user_id   BIGINT REFERENCES users (id),
  friend_id BIGINT REFERENCES users (id)
);

# --- !Downs

DROP TABLE events;
DROP TABLE event_flags;
DROP TABLE event_joins;
DROP TABLE tokens;
DROP TABLE user_friends;
DROP TABLE users;