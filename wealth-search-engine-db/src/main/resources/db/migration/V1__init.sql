CREATE TABLE clients (
    id BINARY(16) PRIMARY KEY,
    first_name VARCHAR(128) NOT NULL,
    last_name VARCHAR(128) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    country_of_residence VARCHAR(120)
);

CREATE TABLE documents (
    id BINARY(16) PRIMARY KEY,
    client_id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_documents_clients FOREIGN KEY (client_id) REFERENCES clients (id)
);

CREATE INDEX idx_clients_email ON clients(email);
CREATE INDEX idx_documents_client_id ON documents(client_id);

