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

import com.wealthsearch.model.DocumentSearchHit;
import com.wealthsearch.model.PaginationParams;
import com.wealthsearch.model.SearchResult;
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
        UUID id = Optional.ofNullable(document.getId())
                          .orElseGet(UUID::randomUUID);
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
                  .fetch()
                  .into(Document.class);
    }

    @Override
    public SearchResult<DocumentSearchHit> searchByContent(List<String> searchTerms, PaginationParams pagination) {
        if (searchTerms == null || searchTerms.isEmpty()) {
            return emptySearchResult();
        }

        var queryContext = buildFullTextSearchContext(searchTerms);
        long totalCount = countMatches(queryContext);
        List<DocumentSearchHit> results = fetchRankedResults(queryContext, pagination);

        return SearchResult.<DocumentSearchHit>builder()
                           .results(results)
                           .totalCount(totalCount)
                           .build();
    }

    private FullTextSearchContext buildFullTextSearchContext(List<String> searchTerms) {
        String searchQuery = String.join(" OR ", searchTerms);
        var tsquery = createTsQuery(searchQuery);
        var tsvField = DSL.field("tsv");
        var matchCondition = DSL.condition("{0} @@ {1}", tsvField, tsquery);
        var rankField = calculateRank(tsvField, tsquery);

        return new FullTextSearchContext(tsquery, matchCondition, rankField);
    }

    private org.jooq.Field<Object> createTsQuery(String searchQuery) {
        return DSL.function("websearch_to_tsquery", Object.class, DSL.inline("english"), DSL.inline(searchQuery));
    }

    private org.jooq.Field<Double> calculateRank(org.jooq.Field<?> tsvField, org.jooq.Field<Object> tsquery) {
        return DSL.function("ts_rank_cd", Double.class, tsvField, tsquery, DSL.inline(4))
                  .as("rank");
    }

    private long countMatches(FullTextSearchContext context) {
        return dsl.selectCount()
                  .from(DOCUMENTS)
                  .where(context.matchCondition)
                  .fetchOne(0, long.class);
    }

    private List<DocumentSearchHit> fetchRankedResults(FullTextSearchContext context, PaginationParams pagination) {
        return dsl.select(DOCUMENTS.asterisk(), context.rankField)
                  .from(DOCUMENTS)
                  .where(context.matchCondition)
                  .orderBy(context.rankField.desc(), DOCUMENTS.CREATED_AT.desc())
                  .limit(pagination.getLimit())
                  .offset(pagination.getOffset())
                  .fetch()
                  .map(this::mapToDocumentSearchHit);
    }

    private DocumentSearchHit mapToDocumentSearchHit(Record record) {
        Document document = mapDocument(record);
        double score = record.get("rank", Double.class);
        return DocumentSearchHit.builder()
                                .document(document)
                                .score(score)
                                .build();
    }

    private SearchResult<DocumentSearchHit> emptySearchResult() {
        return SearchResult.<DocumentSearchHit>builder()
                           .results(List.of())
                           .totalCount(0)
                           .build();
    }

    private record FullTextSearchContext(org.jooq.Field<Object> tsquery, org.jooq.Condition matchCondition,
            org.jooq.Field<Double> rankField) {
    }


    private boolean recordExists(UUID id) {
        return dsl.fetchExists(dsl.selectOne()
                                  .from(DOCUMENTS)
                                  .where(DOCUMENTS.ID.eq(id)));
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
