package com.wealthsearch.db.repository;

import static com.wealthsearch.db.jooq.tables.Documents.DOCUMENTS;

import com.wealthsearch.db.jooq.tables.records.DocumentsRecord;
import com.wealthsearch.db.repository.exception.EntityAlreadyExistsException;
import com.wealthsearch.model.Document;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JooqDocumentRepository implements DocumentRepository {

    private final DSLContext dsl;

    @Override
    public Document save(Document document) {
        UUID id = Optional.ofNullable(document.getId()).orElseGet(UUID::randomUUID);
        if (recordExists(id)) {
            throw new EntityAlreadyExistsException("Document with id '%s' already exists".formatted(id));
        }

        OffsetDateTime createdAt = Optional.ofNullable(document.getCreatedAt())
            .map(this::toUtc)
            .orElseGet(() -> OffsetDateTime.now(ZoneOffset.UTC));

        DocumentsRecord record = dsl.insertInto(DOCUMENTS)
            .set(DOCUMENTS.ID, id)
            .set(DOCUMENTS.CLIENT_ID, document.getClientId())
            .set(DOCUMENTS.TITLE, document.getTitle())
            .set(DOCUMENTS.CONTENT, document.getContent())
            .set(DOCUMENTS.CREATED_AT, createdAt)
            .returning()
            .fetchOptional()
            .orElseThrow(() -> new IllegalStateException("Failed to insert document"));

        return mapDocument(record);
    }

    @Override
    public List<Document> findByClientId(UUID clientId) {
        return dsl.selectFrom(DOCUMENTS)
            .where(DOCUMENTS.CLIENT_ID.eq(clientId))
            .orderBy(DOCUMENTS.CREATED_AT.desc())
            .fetch(this::mapDocument);
    }

    @Override
    public List<Document> searchByContent(String searchTerm) {
        String likePattern = "%" + searchTerm.toLowerCase() + "%";
        return dsl.selectFrom(DOCUMENTS)
            .where(DSL.lower(DOCUMENTS.CONTENT).like(likePattern)
                .or(DSL.lower(DOCUMENTS.TITLE).like(likePattern)))
            .orderBy(DOCUMENTS.CREATED_AT.desc())
            .fetch(this::mapDocument);
    }

    private boolean recordExists(UUID id) {
        return dsl.fetchExists(dsl.selectOne().from(DOCUMENTS).where(DOCUMENTS.ID.eq(id)));
    }

    private Document mapDocument(Record record) {
        DocumentsRecord documentsRecord = record.into(DOCUMENTS);
        return Document.builder()
            .id(documentsRecord.getId())
            .clientId(documentsRecord.getClientId())
            .title(documentsRecord.getTitle())
            .content(documentsRecord.getContent())
            .createdAt(toUtc(documentsRecord.getCreatedAt()))
            .build();
    }

    private OffsetDateTime toUtc(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
