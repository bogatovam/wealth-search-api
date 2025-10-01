package com.wealthsearch.db.repository;

import static com.wealthsearch.db.jooq.tables.Documents.DOCUMENTS;
import static com.wealthsearch.db.repository.util.UuidByteConverter.fromBytes;
import static com.wealthsearch.db.repository.util.UuidByteConverter.toBytes;

import com.wealthsearch.db.jooq.tables.records.DocumentsRecord;
import com.wealthsearch.model.Document;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JooqDocumentRepository implements DocumentRepository {

    private final DSLContext dsl;

    @Override
    public Document save(Document document) {
        UUID id = Optional.ofNullable(document.getId()).orElseGet(UUID::randomUUID);
        OffsetDateTime createdAt = Optional.ofNullable(document.getCreatedAt()).orElseGet(OffsetDateTime::now);
        Document persisted = document.toBuilder().id(id).createdAt(createdAt).build();

        int updated = dsl.update(DOCUMENTS)
            .set(DOCUMENTS.CLIENT_ID, toBytes(persisted.getClientId()))
            .set(DOCUMENTS.TITLE, persisted.getTitle())
            .set(DOCUMENTS.CONTENT, persisted.getContent())
            .set(DOCUMENTS.CREATED_AT, toLocalDateTime(persisted.getCreatedAt()))
            .where(DOCUMENTS.ID.eq(toBytes(id)))
            .execute();

        if (updated == 0) {
            dsl.insertInto(DOCUMENTS)
                .set(DOCUMENTS.ID, toBytes(id))
                .set(DOCUMENTS.CLIENT_ID, toBytes(persisted.getClientId()))
                .set(DOCUMENTS.TITLE, persisted.getTitle())
                .set(DOCUMENTS.CONTENT, persisted.getContent())
                .set(DOCUMENTS.CREATED_AT, toLocalDateTime(persisted.getCreatedAt()))
                .execute();
        }

        return persisted;
    }

    @Override
    public List<Document> findByClientId(UUID clientId) {
        return dsl.selectFrom(DOCUMENTS)
            .where(DOCUMENTS.CLIENT_ID.eq(toBytes(clientId)))
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

    private Document mapDocument(DocumentsRecord record) {
        return Document.builder()
            .id(fromBytes(record.getId()))
            .clientId(fromBytes(record.getClientId()))
            .title(record.getTitle())
            .content(record.getContent())
            .createdAt(toOffsetDateTime(record.getCreatedAt()))
            .build();
    }

    private java.time.LocalDateTime toLocalDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDateTime();
    }

    private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atOffset(ZoneOffset.UTC);
    }
}
