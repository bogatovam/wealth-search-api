package com.wealthsearch.db.repository;

import static com.wealthsearch.db.jooq.tables.Clients.CLIENTS;

import com.wealthsearch.db.jooq.tables.records.ClientsRecord;
import com.wealthsearch.db.repository.exception.DuplicateClientEmailException;
import com.wealthsearch.db.repository.exception.EntityAlreadyExistsException;
import com.wealthsearch.model.Client;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
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

        return mapClient(record);
    }

    @Override
    public Optional<Client> findById(UUID clientId) {
        return dsl.selectFrom(CLIENTS)
                  .where(CLIENTS.ID.eq(clientId))
                  .fetchOptional(this::mapClient);
    }

    @Override
    public List<Client> findByEmailDomainFragment(String domain) {
        String normalized = domain.toLowerCase(Locale.ROOT)
                                  .replaceAll("[^a-z0-9]", "");
        if (normalized.isEmpty()) {
            return List.of();
        }
        String likePattern = "%" + normalized + "%";

        Field<String> sanitizedEmail =
                DSL.replace(DSL.replace(DSL.replace(DSL.lower(CLIENTS.EMAIL), "@", ""), ".", ""), "-", "");

        return dsl.selectFrom(CLIENTS)
                  .where(sanitizedEmail.like(likePattern))
                  .fetch(this::mapClient);
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

    private Client mapClient(Record record) {
        ClientsRecord clientsRecord = record.into(CLIENTS);
        return Client.builder()
                     .id(clientsRecord.getId())
                     .firstName(clientsRecord.getFirstName())
                     .lastName(clientsRecord.getLastName())
                     .email(clientsRecord.getEmail())
                     .countryOfResidence(clientsRecord.getCountryOfResidence())
                     .createdAt(toUtc(clientsRecord.getCreatedAt()))
                     .build();
    }

    private OffsetDateTime toUtc(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
