CREATE TABLE document_summary_process_items (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id),
    status VARCHAR(32) NOT NULL,
    summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX document_summary_process_items_document_idx
    ON document_summary_process_items(document_id);
