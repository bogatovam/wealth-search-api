package com.wealthsearch.db.repository;

import com.wealthsearch.model.entity.DocumentSummaryProcessItem;
import com.wealthsearch.model.entity.DocumentSummaryProcessStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DocumentSummaryProcessItemRepository {

    Optional<DocumentSummaryProcessItem> findActive(UUID documentId);

    Optional<DocumentSummaryProcessItem> findActiveForUpdate(UUID documentId);

    Optional<DocumentSummaryProcessItem> findLatest(UUID documentId);

    DocumentSummaryProcessItem insert(UUID documentId, DocumentSummaryProcessStatus status, OffsetDateTime createdAt);

    DocumentSummaryProcessItem insertEventOrReturnExisting(UUID documentId);

    DocumentSummaryProcessItem markStatus(UUID processItemId, DocumentSummaryProcessStatus status);

    void complete(UUID id, String summary);
}
