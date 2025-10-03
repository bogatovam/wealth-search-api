package com.wealthsearch.db.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wealthsearch.db.config.JooqSettingsConfiguration;
import com.wealthsearch.model.exception.DuplicateClientEmailException;
import com.wealthsearch.db.repository.support.PostgresContainerSupport;
import com.wealthsearch.model.entity.Client;
import com.wealthsearch.model.entity.search.ClientSearchHit;
import com.wealthsearch.model.entity.search.PaginationParams;
import com.wealthsearch.model.entity.search.SearchResult;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jooq.JooqTest;
import org.springframework.context.annotation.Import;

@JooqTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    JooqClientRepository.class,
    JooqSettingsConfiguration.class
})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class JooqClientRepositoryTest extends PostgresContainerSupport {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void saveAssignsIdentifierTimestampAndPersists() {
        var saved = clientRepository.save(Client.builder()
                                                .firstName("Ivan")
                                                .lastName("Ivanov")
                                                .email("Ivan.Ivanov@neviswealth.com")
                                                .countryOfResidence("US")
                                                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getDomainName()).isEqualTo("neviswealth");
        assertThat(clientRepository.findById(saved.getId())).contains(saved);
    }

    @Test
    void saveNormalizesProvidedCreatedAtToUtc() {
        var customTime = OffsetDateTime.now(ZoneOffset.ofHours(-5))
                                       .minusDays(1);

        var saved = clientRepository.save(Client.builder()
                                                .firstName("Time")
                                                .lastName("Traveler")
                                                .email("time.traveler@neviswealth.com")
                                                .countryOfResidence("US")
                                                .createdAt(customTime)
                                                .build());

        assertThat(saved.getCreatedAt()).isEqualTo(customTime.withOffsetSameInstant(ZoneOffset.UTC));
    }

    @Test
    void saveExtractsDomainNameFromCompoundSuffix() {
        var saved = clientRepository.save(Client.builder()
                                                .firstName("Domain")
                                                .lastName("Tester")
                                                .email("domain.tester@shoreline.uk.com")
                                                .countryOfResidence("US")
                                                .build());

        assertThat(saved.getDomainName()).isEqualTo("shorelineuk");
    }

    @Test
    void saveRejectsDuplicateEmail() {
        clientRepository.save(Client.builder()
                                    .firstName("Nevis")
                                    .lastName("Advisor")
                                    .email("insights@neviswealth.com")
                                    .countryOfResidence("US")
                                    .build());

        assertThatThrownBy(() -> clientRepository.save(Client.builder()
                                                             .firstName("Copy")
                                                             .lastName("Advisor")
                                                             .email("insights@neviswealth.com")
                                                             .countryOfResidence("US")
                                                             .build())).isInstanceOf(DuplicateClientEmailException.class);
    }

    @Test
    void findByEmailDomainFragmentMatchesNormalizedDomain() {
        var match = clientRepository.save(Client.builder()
                                                .firstName("Nevis")
                                                .lastName("Advisor")
                                                .email("insights@neviswealth.com")
                                                .countryOfResidence("US")
                                                .build());

        clientRepository.save(Client.builder()
                                    .firstName("Other")
                                    .lastName("Person")
                                    .email("support@wealthbridge.ai")
                                    .countryOfResidence("US")
                                    .build());

        SearchResult<ClientSearchHit> searchResult = clientRepository.findClientsByCompanyDomain(
                List.of("neviswealth"),
                PaginationParams.of(10, 0));
        assertThat(searchResult.getResults()).extracting(ClientSearchHit::getClient)
                                              .extracting(Client::getId)
                                              .containsExactly(match.getId());
        assertThat(searchResult.getTotalCount()).isEqualTo(1);
    }

    @SpringBootConfiguration
    static class TestConfig {}
}
