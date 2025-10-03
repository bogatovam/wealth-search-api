package com.wealthsearch.db.repository;

import static com.wealthsearch.db.jooq.tables.Documents.DOCUMENTS;

import com.wealthsearch.db.jooq.tables.records.DocumentsRecord;
import com.wealthsearch.model.exception.EntityAlreadyExistsException;
import com.wealthsearch.model.entity.Document;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

import com.wealthsearch.model.entity.search.DocumentSearchHit;
import com.wealthsearch.model.entity.search.PaginationParams;
import com.wealthsearch.model.entity.search.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Slf4j
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

        return record.into(Document.class);
    }

    @Override
    public Optional<Document> findById(UUID documentId) {
        return dsl.selectFrom(DOCUMENTS)
                .where(DOCUMENTS.ID.eq(documentId))
                .fetchOptional()
                .map(record -> record.into(Document.class));
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
    public SearchResult<DocumentSearchHit> searchByContent(Set<String> searchTerms, PaginationParams pagination) {
        if (searchTerms == null || searchTerms.isEmpty()) {
            return emptySearchResult();
        }

        var queryContext = buildFullTextSearchContext(searchTerms);
        long totalCount = countMatches(queryContext);
        if (totalCount > 0) {
            List<DocumentSearchHit> results = fetchRankedResults(queryContext, pagination);

            return SearchResult.<DocumentSearchHit>builder()
                               .results(results)
                               .totalCount(totalCount)
                               .build();
        } else {
            return SearchResult.<DocumentSearchHit>builder()
                               .results(new ArrayList<>())
                               .totalCount(totalCount)
                               .build();
        }
    }

    private FullTextSearchContext buildFullTextSearchContext(Set<String> searchTerms) {
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
        return DSL.function("ts_rank_cd", Double.class, tsvField, tsquery, DSL.inline(0))
                  .as("rank");
    }

    private long countMatches(FullTextSearchContext context) {
        return dsl.selectCount()
                  .from(DOCUMENTS)
                  .where(context.matchCondition)
                  .fetchOne(0, long.class);
    }

    private List<DocumentSearchHit> fetchRankedResults(FullTextSearchContext context, PaginationParams pagination) {
        List<Field<?>> fieldsForSelect = Stream.concat(DOCUMENTS.fieldStream(), Stream.of(context.rankField))
                                               .toList();;

        var query = dsl.select(fieldsForSelect)
                       .from(DOCUMENTS)
                       .where(context.matchCondition)
                       .orderBy(context.rankField.desc(), DOCUMENTS.CREATED_AT.desc())
                       .limit(pagination.getLimit())
                       .offset(pagination.getOffset());

        log.info("SQL Query: {}", query.getSQL(ParamType.INLINED));

        return query.fetch()
                    .map(this::mapToDocumentSearchHit);
    }

    private DocumentSearchHit mapToDocumentSearchHit(Record record) {
        Document document = record.into(Document.class);
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

    private boolean recordExists(UUID id) {
        return dsl.fetchExists(dsl.selectOne()
                                  .from(DOCUMENTS)
                                  .where(DOCUMENTS.ID.eq(id)));
    }

    private OffsetDateTime toUtc(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.withOffsetSameInstant(ZoneOffset.UTC);
    }

    private record FullTextSearchContext(Field<Object> tsquery, Condition matchCondition, Field<Double> rankField) { }
}
