package com.wealthsearch.db.repository;

import static com.wealthsearch.db.jooq.tables.DocumentSummaryProcessItems.DOCUMENT_SUMMARY_PROCESS_ITEMS;

import com.wealthsearch.db.jooq.tables.records.DocumentSummaryProcessItemsRecord;
import com.wealthsearch.model.entity.DocumentSummaryProcessItem;
import com.wealthsearch.model.entity.DocumentSummaryProcessStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JooqDocumentSummaryProcessItemRepository implements DocumentSummaryProcessItemRepository {

    private final DSLContext dsl;

    @Override
    public Optional<DocumentSummaryProcessItem> findById(UUID documentId) {
        return selectBase(documentId).orderBy(DOCUMENT_SUMMARY_PROCESS_ITEMS.CREATED_AT.desc())
                                     .fetchOptional()
                                     .map(r -> r.into(DocumentSummaryProcessItem.class));
    }

    @Override
    public DocumentSummaryProcessItem markStatus(UUID documentId, DocumentSummaryProcessStatus status) {
        return dsl.update(DOCUMENT_SUMMARY_PROCESS_ITEMS)
                  .set(DOCUMENT_SUMMARY_PROCESS_ITEMS.STATUS, status.name())
                  .where(DOCUMENT_SUMMARY_PROCESS_ITEMS.DOCUMENT_ID.eq(documentId))
                  .returning()
                  .fetchOne()
                  .into(DocumentSummaryProcessItem.class);
    }

    @Override
    public void complete(UUID documentId, String summary) {
        dsl.update(DOCUMENT_SUMMARY_PROCESS_ITEMS)
           .set(DOCUMENT_SUMMARY_PROCESS_ITEMS.STATUS, DocumentSummaryProcessStatus.COMPLETED.name())
           .set(DOCUMENT_SUMMARY_PROCESS_ITEMS.COMPLETED_AT, OffsetDateTime.now())
           .set(DOCUMENT_SUMMARY_PROCESS_ITEMS.SUMMARY, summary)
           .where(DOCUMENT_SUMMARY_PROCESS_ITEMS.DOCUMENT_ID.eq(documentId))
           .execute();
    }

    @Override
    public DocumentSummaryProcessItem insertEventOrReturnExisting(UUID documentId) {
        return dsl.insertInto(DOCUMENT_SUMMARY_PROCESS_ITEMS)
                  .set(DOCUMENT_SUMMARY_PROCESS_ITEMS.DOCUMENT_ID, documentId)
                  .set(DOCUMENT_SUMMARY_PROCESS_ITEMS.STATUS, DocumentSummaryProcessStatus.IN_PROGRESS.name())
                  .set(DOCUMENT_SUMMARY_PROCESS_ITEMS.CREATED_AT, OffsetDateTime.now())
                  .returning()
                  .fetchOptional(r -> r.into(DocumentSummaryProcessItem.class))
                  .orElseThrow(() -> new IllegalStateException("Failed to insert document summary process item"));
    }

    private SelectConditionStep<DocumentSummaryProcessItemsRecord> selectBase(UUID documentId) {
        return dsl.selectFrom(DOCUMENT_SUMMARY_PROCESS_ITEMS)
                  .where(DOCUMENT_SUMMARY_PROCESS_ITEMS.DOCUMENT_ID.eq(documentId));
    }
}
