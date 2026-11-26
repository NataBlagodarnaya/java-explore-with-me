CREATE TABLE IF NOT EXISTS users (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(250) NOT NULL,
    email VARCHAR(254) NOT NULL,
    CONSTRAINT uq_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS categories (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uq_category_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS events (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title              VARCHAR(120)  NOT NULL,
    annotation         VARCHAR(2000) NOT NULL,
    description        VARCHAR(7000) NOT NULL,
    category_id        BIGINT NOT NULL REFERENCES categories (id),
    initiator_id       BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    event_date         TIMESTAMP NOT NULL,
    created_on         TIMESTAMP NOT NULL,
    published_on       TIMESTAMP,
    lat                REAL NOT NULL,
    lon                REAL NOT NULL,
    paid               BOOLEAN NOT NULL DEFAULT FALSE,
    participant_limit  INTEGER NOT NULL DEFAULT 0,
    request_moderation BOOLEAN NOT NULL DEFAULT TRUE,
    state              VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS requests (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created      TIMESTAMP NOT NULL,
    event_id     BIGINT NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    requester_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL,
    CONSTRAINT uq_request UNIQUE (event_id, requester_id)
);

CREATE TABLE IF NOT EXISTS compilations (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title  VARCHAR(50) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_compilation_name UNIQUE (title)
);

CREATE TABLE IF NOT EXISTS compilations_events (
    compilation_id BIGINT NOT NULL REFERENCES compilations (id) ON DELETE CASCADE,
    event_id       BIGINT NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    PRIMARY KEY (compilation_id, event_id)
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscriber_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    CONSTRAINT fk_subscriptions_subscriber FOREIGN KEY (subscriber_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_subscription UNIQUE (subscriber_id, author_id)
);