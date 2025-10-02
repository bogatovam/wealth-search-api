CREATE TABLE countries (
    code        VARCHAR(2)  PRIMARY KEY,
    name        VARCHAR(64) NOT NULL
);

INSERT INTO countries (code, name) VALUES
    ('US', 'United States'),
    ('CH', 'Switzerland'),
    ('GB', 'United Kingdom'),
    ('CA', 'Canada'),
    ('DE', 'Germany');

CREATE TABLE clients (
    id                   UUID                     PRIMARY KEY,
    first_name           VARCHAR(128)             NOT NULL,
    last_name            VARCHAR(128)             NOT NULL,
    email                VARCHAR(320)             NOT NULL UNIQUE,
    country_of_residence VARCHAR(2)               REFERENCES countries(code),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE documents (
    id         UUID                     PRIMARY KEY,
    client_id  UUID                     NOT NULL REFERENCES clients(id),
    title      VARCHAR(255)             NOT NULL,
    content    TEXT                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
