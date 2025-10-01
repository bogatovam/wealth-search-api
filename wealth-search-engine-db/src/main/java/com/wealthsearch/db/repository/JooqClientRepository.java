package com.wealthsearch.db.repository;

import static com.wealthsearch.db.jooq.tables.Clients.CLIENTS;
import static com.wealthsearch.db.repository.util.UuidByteConverter.fromBytes;
import static com.wealthsearch.db.repository.util.UuidByteConverter.toBytes;

import com.wealthsearch.db.jooq.tables.records.ClientsRecord;
import com.wealthsearch.model.Client;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JooqClientRepository implements ClientRepository {

    private final DSLContext dsl;

    @Override
    public Client save(Client client) {
        UUID id = Optional.ofNullable(client.getId()).orElseGet(UUID::randomUUID);
        Client persisted = client.toBuilder().id(id).build();

        int updated = dsl.update(CLIENTS)
            .set(CLIENTS.FIRST_NAME, persisted.getFirstName())
            .set(CLIENTS.LAST_NAME, persisted.getLastName())
            .set(CLIENTS.EMAIL, persisted.getEmail())
            .set(CLIENTS.COUNTRY_OF_RESIDENCE, persisted.getCountryOfResidence())
            .where(CLIENTS.ID.eq(toBytes(id)))
            .execute();

        if (updated == 0) {
            dsl.insertInto(CLIENTS)
                .set(CLIENTS.ID, toBytes(id))
                .set(CLIENTS.FIRST_NAME, persisted.getFirstName())
                .set(CLIENTS.LAST_NAME, persisted.getLastName())
                .set(CLIENTS.EMAIL, persisted.getEmail())
                .set(CLIENTS.COUNTRY_OF_RESIDENCE, persisted.getCountryOfResidence())
                .execute();
        }

        return persisted;
    }

    @Override
    public Optional<Client> findById(UUID clientId) {
        return dsl.selectFrom(CLIENTS)
            .where(CLIENTS.ID.eq(toBytes(clientId)))
            .fetchOptional(this::mapClient);
    }

    @Override
    public List<Client> findByEmailDomainFragment(String domain) {
        String normalized = domain.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (normalized.isEmpty()) {
            return List.of();
        }
        String likePattern = "%" + normalized + "%";
        var sanitizedEmail = DSL.replace(DSL.replace(DSL.lower(CLIENTS.EMAIL), "@", ""), ".", "");
        sanitizedEmail = DSL.replace(sanitizedEmail, "-", "");
        return dsl.selectFrom(CLIENTS)
            .where(sanitizedEmail.like(likePattern))
            .fetch(this::mapClient);
    }

    private Client mapClient(ClientsRecord record) {
        return Client.builder()
            .id(fromBytes(record.getId()))
            .firstName(record.getFirstName())
            .lastName(record.getLastName())
            .email(record.getEmail())
            .countryOfResidence(record.getCountryOfResidence())
            .build();
    }
}


