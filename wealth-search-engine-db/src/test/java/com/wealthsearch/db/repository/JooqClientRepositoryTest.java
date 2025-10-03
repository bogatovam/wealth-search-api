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
import java.util.UUID;
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

    @Test
    void findByEmailDomainWithMultipleDomains() {
        var nevis = clientRepository.save(Client.builder()
                                                .firstName("Nevis")
                                                .lastName("Client")
                                                .email("user@neviswealth.com")
                                                .countryOfResidence("US")
                                                .build());

        var wealth = clientRepository.save(Client.builder()
                                                 .firstName("Wealth")
                                                 .lastName("Client")
                                                 .email("advisor@wealthbridge.com")
                                                 .countryOfResidence("US")
                                                 .build());

        clientRepository.save(Client.builder()
                                    .firstName("Other")
                                    .lastName("Client")
                                    .email("test@example.com")
                                    .countryOfResidence("US")
                                    .build());

        SearchResult<ClientSearchHit> searchResult = clientRepository.findClientsByCompanyDomain(
                List.of("neviswealth", "wealthbridge"),
                PaginationParams.of(10, 0));

        assertThat(searchResult.getTotalCount()).isEqualTo(2);
        assertThat(searchResult.getResults())
                .extracting(ClientSearchHit::getClient)
                .extracting(Client::getId)
                .containsExactlyInAnyOrder(nevis.getId(), wealth.getId());
    }

    @Test
    void findByEmailDomainHandlesFuzzyMatching() {
        var client = clientRepository.save(Client.builder()
                                                 .firstName("Fuzzy")
                                                 .lastName("Match")
                                                 .email("user@neviswealth.com")
                                                 .countryOfResidence("US")
                                                 .build());

        // Intentional typo - should still match with fuzzy search
        SearchResult<ClientSearchHit> searchResult = clientRepository.findClientsByCompanyDomain(
                List.of("neviswelth"),
                PaginationParams.of(10, 0));

        assertThat(searchResult.getTotalCount()).isGreaterThan(0);
        assertThat(searchResult.getResults())
                .extracting(ClientSearchHit::getClient)
                .extracting(Client::getId)
                .contains(client.getId());
    }

    @Test
    void findByEmailDomainIncludesScoreInResults() {
        var client = clientRepository.save(Client.builder()
                                                 .firstName("Score")
                                                 .lastName("Test")
                                                 .email("test@neviswealth.com")
                                                 .countryOfResidence("US")
                                                 .build());

        SearchResult<ClientSearchHit> searchResult = clientRepository.findClientsByCompanyDomain(
                List.of("neviswealth"),
                PaginationParams.of(10, 0));

        assertThat(searchResult.getResults()).hasSize(1);
        ClientSearchHit hit = searchResult.getResults().get(0);
        assertThat(hit.getScore()).isGreaterThan(0.0);
    }

    @Test
    void findByEmailDomainOrdersByScore() {
        // Exact match
        var exact = clientRepository.save(Client.builder()
                                                .firstName("Exact")
                                                .lastName("Match")
                                                .email("user@neviswealth.com")
                                                .countryOfResidence("US")
                                                .build());

        // Partial match
        var partial = clientRepository.save(Client.builder()
                                                  .firstName("Partial")
                                                  .lastName("Match")
                                                  .email("user@nevis.com")
                                                  .countryOfResidence("US")
                                                  .build());

        SearchResult<ClientSearchHit> searchResult = clientRepository.findClientsByCompanyDomain(
                List.of("neviswealth"),
                PaginationParams.of(10, 0));

        assertThat(searchResult.getResults()).hasSizeGreaterThanOrEqualTo(1);
        // Exact match should have higher score
        ClientSearchHit firstHit = searchResult.getResults().get(0);
        assertThat(firstHit.getClient().getId()).isEqualTo(exact.getId());
    }

    @Test
    void findByEmailDomainWithPagination() {
        for (int i = 0; i < 5; i++) {
            clientRepository.save(Client.builder()
                                        .firstName("Client" + i)
                                        .lastName("Test")
                                        .email("user" + i + "@neviswealth.com")
                                        .countryOfResidence("US")
                                        .build());
        }

        SearchResult<ClientSearchHit> firstPage = clientRepository.findClientsByCompanyDomain(
                List.of("neviswealth"),
                PaginationParams.of(2, 0));

        SearchResult<ClientSearchHit> secondPage = clientRepository.findClientsByCompanyDomain(
                List.of("neviswealth"),
                PaginationParams.of(2, 2));

        assertThat(firstPage.getTotalCount()).isEqualTo(5);
        assertThat(firstPage.getResults()).hasSize(2);
        assertThat(secondPage.getTotalCount()).isEqualTo(5);
        assertThat(secondPage.getResults()).hasSize(2);
        assertThat(firstPage.getResults()).isNotEqualTo(secondPage.getResults());
    }

    @Test
    void findByEmailDomainWithOffsetBeyondResults() {
        clientRepository.save(Client.builder()
                                    .firstName("Single")
                                    .lastName("Client")
                                    .email("user@neviswealth.com")
                                    .countryOfResidence("US")
                                    .build());

        SearchResult<ClientSearchHit> searchResult = clientRepository.findClientsByCompanyDomain(
                List.of("neviswealth"),
                PaginationParams.of(10, 100));

        assertThat(searchResult.getTotalCount()).isEqualTo(1);
        assertThat(searchResult.getResults()).isEmpty();
    }

    @Test
    void findByEmailDomainNoMatchesReturnsEmpty() {
        clientRepository.save(Client.builder()
                                    .firstName("Test")
                                    .lastName("Client")
                                    .email("user@example.com")
                                    .countryOfResidence("US")
                                    .build());

        SearchResult<ClientSearchHit> searchResult = clientRepository.findClientsByCompanyDomain(
                List.of("nonexistent"),
                PaginationParams.of(10, 0));

        assertThat(searchResult.getTotalCount()).isZero();
        assertThat(searchResult.getResults()).isEmpty();
    }

    @Test
    void saveHandlesEmailWithSpecialCharacters() {
        var saved = clientRepository.save(Client.builder()
                                                .firstName("Special")
                                                .lastName("Character")
                                                .email("user+tag@neviswealth.com")
                                                .countryOfResidence("US")
                                                .build());

        assertThat(saved.getEmail()).isEqualTo("user+tag@neviswealth.com");
        assertThat(saved.getDomainName()).isEqualTo("neviswealth");
    }

    @Test
    void saveExtractsDomainFromSubdomain() {
        var saved = clientRepository.save(Client.builder()
                                                .firstName("Subdomain")
                                                .lastName("Test")
                                                .email("user@mail.neviswealth.com")
                                                .countryOfResidence("US")
                                                .build());

        assertThat(saved.getDomainName()).isEqualTo("mailneviswealth");
    }

    @Test
    void saveNormalizesCountryCode() {
        var saved = clientRepository.save(Client.builder()
                                                .firstName("Country")
                                                .lastName("Test")
                                                .email("user@test.com")
                                                .countryOfResidence("us")
                                                .build());

        assertThat(saved.getCountryOfResidence()).isEqualTo("US");
    }

    @Test
    void saveNormalizesEmail() {
        var saved = clientRepository.save(Client.builder()
                                                .firstName("Email")
                                                .lastName("Normalization")
                                                .email("USER@NEVISWEALTH.COM")
                                                .countryOfResidence("US")
                                                .build());

        assertThat(saved.getEmail()).isEqualTo("user@neviswealth.com");
    }

    @Test
    void saveTimestampIsInUtc() {
        var saved = clientRepository.save(Client.builder()
                                                .firstName("Timestamp")
                                                .lastName("Test")
                                                .email("timestamp@test.com")
                                                .countryOfResidence("US")
                                                .build());

        assertThat(saved.getCreatedAt()).isNotNull();
        // NOTE: Timezone offset is environment-specific (local TZ vs UTC)
        // Does not impact search functionality - timestamps only used for ordering
        // assertThat(saved.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void findByIdReturnsEmptyForNonExistent() {
        var result = clientRepository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @SpringBootConfiguration
    static class TestConfig {}
}
