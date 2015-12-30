CREATE TABLE events (
  id         BIGINT NOT NULL,
  name       TEXT,
  start_date BIGINT,
  end_date   BIGINT,
  "offset"   BIGINT,
  location   TEXT,
  url        TEXT
);

CREATE SEQUENCE event_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

ALTER SEQUENCE event_id_seq OWNED BY events.id;

CREATE TABLE event_flags (
  event_id BIGINT,
  user_id  BIGINT
);

CREATE TABLE event_joins (
  event_id BIGINT,
  user_id  BIGINT
);

CREATE TABLE tokens (
  token   TEXT,
  user_id BIGINT NOT NULL
);

CREATE TABLE user_friends (
  user_id   BIGINT,
  friend_id BIGINT
);

CREATE TABLE users (
  id    BIGINT NOT NULL,
  login TEXT
);

ALTER TABLE ONLY events ALTER COLUMN id SET DEFAULT nextval('event_id_seq' :: REGCLASS);

ALTER TABLE ONLY events ADD CONSTRAINT event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tokens ADD CONSTRAINT tokens_user_id_pk PRIMARY KEY (user_id);

ALTER TABLE ONLY users ADD CONSTRAINT users_id_pk PRIMARY KEY (id);

CREATE UNIQUE INDEX event_id_uindex ON events USING BTREE (id);

CREATE UNIQUE INDEX tokens_user_id_uindex ON tokens USING BTREE (user_id);

CREATE UNIQUE INDEX users_id_uindex ON users USING BTREE (id);