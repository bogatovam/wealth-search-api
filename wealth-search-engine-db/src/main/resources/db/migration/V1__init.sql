CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE clients (
    id                   UUID                     PRIMARY KEY,
    first_name           VARCHAR(128)             NOT NULL,
    last_name            VARCHAR(128)             NOT NULL,
    email                VARCHAR(320)             NOT NULL UNIQUE,
    country_of_residence VARCHAR(2)               NOT NULL,
    domain_name          VARCHAR(128)             NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

--CREATE TABLE company_domains (
--    name                 VARCHAR(128) PRIMARY KEY,
--    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
--);

CREATE TABLE documents (
    id         UUID                     PRIMARY KEY,
    client_id  UUID                     NOT NULL REFERENCES clients(id),
    title      VARCHAR(255)             NOT NULL,
    content    TEXT                     NOT NULL,
    tsv        tsvector                 NOT NULL GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX documents_tsv_gin ON documents USING gin (tsv);
