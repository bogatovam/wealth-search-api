package com.wealthsearch.db.repository;

import static com.wealthsearch.db.jooq.tables.Clients.CLIENTS;

import com.wealthsearch.db.jooq.tables.records.ClientsRecord;
import com.wealthsearch.db.repository.exception.DuplicateClientEmailException;
import com.wealthsearch.db.repository.exception.EntityAlreadyExistsException;
import com.wealthsearch.model.Client;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import com.wealthsearch.model.ClientSearchHit;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

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

        String countryCode = Optional.ofNullable(client.getCountryOfResidence())
                                     .map(code -> code.toUpperCase(Locale.ROOT))
                                     .orElse(null);

        OffsetDateTime createdAt = Optional.ofNullable(client.getCreatedAt())
                                           .map(this::toUtc)
                                           .orElseGet(() -> OffsetDateTime.now(ZoneOffset.UTC));

        ClientsRecord record = dsl.insertInto(CLIENTS)
                                  .set(CLIENTS.ID, id)
                                  .set(CLIENTS.FIRST_NAME, client.getFirstName())
                                  .set(CLIENTS.LAST_NAME, client.getLastName())
                                  .set(CLIENTS.EMAIL, email)
                                  .set(CLIENTS.COUNTRY_OF_RESIDENCE, countryCode)
                                  .set(CLIENTS.CREATED_AT, createdAt)
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
    public List<ClientSearchHit> findClientsByCompanyDomain(List<String> domains) {
        Field<Double> score = this.createScoreFieldForSelect(CLIENTS.DOMAIN_NAME, domains);

        List<Field<?>> fieldsForSelect = new ArrayList<>(List.of(CLIENTS.fields()));
        fieldsForSelect.add(score);

        return dsl.select(fieldsForSelect)
                  .from(CLIENTS)
                  .where(this.createFuzzyMatchCondition(CLIENTS.DOMAIN_NAME, domains))
                  .orderBy(score)
                  .fetch(record -> mapClient(record, score));
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
                                   Field<Double> strictWord =
                                           DSL.function("strict_word_similarity", SQLDataType.DOUBLE, query, field);
                                   return DSL.greatest(trigram, word, strictWord);
                               })
                               .toList();

        Field<Double> score = DSL.greatest(scores.getFirst(), (Field<?>) scores.subList(1, scores.size()));

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
                                              .or(DSL.condition("{0} <% {1}", query, field))
                                              .or(DSL.condition("{0} <<% {1}", query, field));
            
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

    private ClientSearchHit mapClient(Record record, Field<Double> score) {
        Client client = record.into(Client.class);
        return new ClientSearchHit(client, record.get(score));
    }

    private OffsetDateTime toUtc(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
