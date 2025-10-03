package com.wealthsearch.db.repository;

import static com.wealthsearch.db.jooq.tables.Clients.CLIENTS;

import com.wealthsearch.db.jooq.tables.records.ClientsRecord;
import com.wealthsearch.model.entity.Client;
import com.wealthsearch.model.entity.search.ClientSearchHit;
import com.wealthsearch.model.entity.search.PaginationParams;
import com.wealthsearch.model.entity.search.SearchResult;
import com.wealthsearch.model.exception.DuplicateClientEmailException;
import com.wealthsearch.model.exception.EntityAlreadyExistsException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JooqClientRepository implements ClientRepository {

    private final DSLContext dsl;

    @Override
    public Client save(Client client) {
        UUID id = UUID.randomUUID();

        if (recordExistsById(id)) {
            throw new EntityAlreadyExistsException("Client with id '%s' already exists".formatted(id));
        }

        String email = Optional.ofNullable(client.getEmail())
                               .map(String::toLowerCase)
                               .orElse(null);

        if (email != null && recordExistsByEmail(email)) {
            throw new DuplicateClientEmailException(email);
        }

        String domainName = this.extractDomainName(email);

        String countryCode = Optional.ofNullable(client.getCountryOfResidence())
                                     .map(code -> code.toUpperCase(Locale.ROOT))
                                     .orElse(null);

        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        ClientsRecord record = dsl.insertInto(CLIENTS)
                                  .set(CLIENTS.ID, id)
                                  .set(CLIENTS.FIRST_NAME, client.getFirstName())
                                  .set(CLIENTS.LAST_NAME, client.getLastName())
                                  .set(CLIENTS.EMAIL, email)
                                  .set(CLIENTS.COUNTRY_OF_RESIDENCE, countryCode)
                                  .set(CLIENTS.CREATED_AT, createdAt)
                                  .set(CLIENTS.DOMAIN_NAME, domainName)
                                  .returning()
                                  .fetchOptional()
                                  .orElseThrow(() -> new IllegalStateException("Failed to insert client"));

        return record.into(Client.class);
    }

    @Override
    public Optional<Client> findById(UUID clientId) {
        return dsl.selectFrom(CLIENTS)
                  .where(CLIENTS.ID.eq(clientId))
                  .fetchOptional()
                  .map(r -> r.into(Client.class));
    }

    @Override
    public SearchResult<ClientSearchHit> findClientsByCompanyDomain(List<String> domains, PaginationParams pagination) {
        Condition condition = this.createFuzzyMatchCondition(CLIENTS.DOMAIN_NAME, domains);

        Long totalCount = dsl.selectCount()
                             .from(CLIENTS)
                             .where(condition)
                             .fetchOneInto(Long.class);

        if (totalCount == null || totalCount == 0) {
            return new SearchResult<ClientSearchHit>();
        }

        Field<Double> score = this.createScoreFieldForSelect(CLIENTS.DOMAIN_NAME, domains);

        List<Field<?>> fieldsForSelect = new ArrayList<>(List.of(CLIENTS.fields()));
        fieldsForSelect.add(score);

        var query = dsl.select(fieldsForSelect)
                       .from(CLIENTS)
                       .where(condition)
                       .orderBy(score.desc())
                       .limit(pagination.getLimit())
                       .offset(pagination.getOffset());

        log.info("SQL Query: {}", query.getSQL(ParamType.INLINED));

        List<ClientSearchHit> results = query.fetch(record -> mapClientSearchHit(record, score));

        return SearchResult.<ClientSearchHit>builder()
                           .results(results)
                           .totalCount(totalCount)
                           .build();
    }

    private <T> Field<Double> createScoreFieldForSelect(Field<T> field, List<T> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("At least one candidate must be provided");
        }

        var scores = candidates.stream()
                               .filter(Objects::nonNull)
                               .map(candidate -> {
                                   Field<String> query = DSL.inline(candidate, SQLDataType.VARCHAR);

                                   Field<Double> trigram = DSL.function("similarity", SQLDataType.DOUBLE, field, query);

                                   Field<Double> word =
                                           DSL.function("word_similarity", SQLDataType.DOUBLE, query, field);

                                   return DSL.greatest(trigram, word);
                               })
                               .toList();

        Field<Double> score = DSL.greatest(scores.getFirst(), scores.subList(1, scores.size())
                                                                    .toArray(new Field[0]));

        return score.as("score");
    }

    private Condition createFuzzyMatchCondition(Field<String> field, List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("At least one candidate must be provided");
        }

        Condition combined = DSL.falseCondition();

        for (String candidate : candidates) {
            Field<String> query = DSL.inline(candidate, SQLDataType.VARCHAR);

            Condition candidateCondition = DSL.condition("{0} % {1}", field, query)
                                              .or(DSL.condition("{0} <% {1}", query, field));

            combined = combined.or(candidateCondition);
        }

        return combined;
    }

    private boolean recordExistsById(UUID id) {
        return dsl.fetchExists(dsl.selectOne()
                                  .from(CLIENTS)
                                  .where(CLIENTS.ID.eq(id)));
    }

    private boolean recordExistsByEmail(String email) {
        return dsl.fetchExists(dsl.selectOne()
                                  .from(CLIENTS)
                                  .where(CLIENTS.EMAIL.eq(email)));
    }

    private String extractDomainName(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        String normalized = email.trim()
                                 .toLowerCase(Locale.ROOT);

        int atIndex = normalized.indexOf('@');
        if (atIndex < 0 || atIndex == normalized.length() - 1) {
            throw new IllegalArgumentException("Invalid email format");
        }

        String domainPart = normalized.substring(atIndex + 1);
        if (domainPart.isEmpty()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        String candidate = domainPart;
        int comIndex = domainPart.lastIndexOf(".");
        if (comIndex > 0) {
            candidate = domainPart.substring(0, comIndex);
        }

        candidate = trimTrailingDots(candidate);
        if (candidate.isEmpty()) {
            candidate = domainPart;
        }

        candidate = removeSpecialCharacters(candidate);
        if (!candidate.isEmpty()) {
            return candidate;
        }

        throw new IllegalArgumentException("Cannot derive domain name from email");
    }

    private String removeSpecialCharacters(String label) {
        if (label == null) {
            return "";
        }
        return label.replaceAll("[^a-z0-9]", "");
    }

    private String trimTrailingDots(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '.') {
            end--;
        }
        return value.substring(0, end);
    }

    private Client mapClientRecord(Record record) {
        Client client = record.into(Client.class);
        client.setCreatedAt(toUtc(client.getCreatedAt()));
        return client;
    }


    private ClientSearchHit mapClientSearchHit(Record record, Field<Double> score) {
        Client client = record.into(Client.class);
        return new ClientSearchHit(client, record.get(score));
    }

    private OffsetDateTime toUtc(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
