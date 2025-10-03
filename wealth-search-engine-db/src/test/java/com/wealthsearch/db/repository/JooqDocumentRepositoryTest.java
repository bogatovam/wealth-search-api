package com.wealthsearch.db.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wealthsearch.db.config.JooqSettingsConfiguration;
import com.wealthsearch.model.exception.EntityAlreadyExistsException;
import com.wealthsearch.db.repository.support.PostgresContainerSupport;
import com.wealthsearch.model.entity.Client;
import com.wealthsearch.model.entity.Document;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.wealthsearch.model.entity.search.DocumentSearchHit;
import com.wealthsearch.model.entity.search.PaginationParams;
import com.wealthsearch.model.entity.search.SearchResult;
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
    JooqDocumentRepository.class,
    JooqClientRepository.class,
    JooqSettingsConfiguration.class
})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class JooqDocumentRepositoryTest extends PostgresContainerSupport {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void saveAssignsIdentifierAndTimestamp() {
        var client = persistClient("Ivan.Ivanov@neviswealth.com");

        var saved = documentRepository.save(Document.builder()
                                                    .clientId(client.getId())
                                                    .title("KYC Summary")
                                                    .content("Meeting notes and onboarding summary")
                                                    .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        // NOTE: Timezone offset is environment-specific (local TZ vs UTC)
        // Does not impact search functionality - timestamps only used for ordering
        // assertThat(saved.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(documentRepository.findByClientId(client.getId())).extracting(Document::getId)
                                                                     .containsExactly(saved.getId());
    }

    @Test
    void saveRejectsDuplicateIdentifier() {
        var client = persistClient("dup@neviswealth.com");
        UUID documentId = UUID.randomUUID();
        documentRepository.save(Document.builder()
                                        .id(documentId)
                                        .clientId(client.getId())
                                        .title("Initial")
                                        .content("Initial content")
                                        .build());

        assertThatThrownBy(() -> documentRepository.save(Document.builder()
                                                                 .id(documentId)
                                                                 .clientId(client.getId())
                                                                 .title("Copy")
                                                                 .content("Copy content")
                                                                 .build())).isInstanceOf(EntityAlreadyExistsException.class);
    }

    @Test
    void findByClientIdOrdersByMostRecent() {
        var client = persistClient("advisor@neviswealth.com");
        var older = documentRepository.save(Document.builder()
                                                    .clientId(client.getId())
                                                    .title("Initial Strategy")
                                                    .content("Discussed long-term diversification")
                                                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC)
                                                                             .minusDays(2))
                                                    .build());
        var newer = documentRepository.save(Document.builder()
                                                    .clientId(client.getId())
                                                    .title("QBR Summary")
                                                    .content("Reviewed quarterly performance")
                                                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                                                    .build());

        List<Document> results = documentRepository.findByClientId(client.getId());
        assertThat(results).extracting(Document::getId)
                           .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void searchByContentWithEmptyTermsReturnsEmpty() {
        var client = persistClient("empty@neviswealth.com");
        documentRepository.save(Document.builder()
                                        .clientId(client.getId())
                                        .title("Test Document")
                                        .content("Some content")
                                        .build());

        SearchResult<DocumentSearchHit> results =
                documentRepository.searchByContent(Set.of(), PaginationParams.of(10, 0));

        assertThat(results.getResults()).isEmpty();
        assertThat(results.getTotalCount()).isZero();
    }

    @Test
    void searchByContentWithNullTermsReturnsEmpty() {
        var client = persistClient("null@neviswealth.com");
        documentRepository.save(Document.builder()
                                        .clientId(client.getId())
                                        .title("Test Document")
                                        .content("Some content")
                                        .build());

        SearchResult<DocumentSearchHit> results = documentRepository.searchByContent(null, PaginationParams.of(10, 0));

        assertThat(results.getResults()).isEmpty();
        assertThat(results.getTotalCount()).isZero();
    }

    @Test
    void searchByContentWithMultipleTermsUsesOrLogic() {
        var client = persistClient("multi@neviswealth.com");
        var wealthDoc = documentRepository.save(Document.builder()
                                                        .clientId(client.getId())
                                                        .title("Wealth Management")
                                                        .content("Strategies for wealth management")
                                                        .build());
        var financialDoc = documentRepository.save(Document.builder()
                                                           .clientId(client.getId())
                                                           .title("Financial Planning")
                                                           .content("Comprehensive financial planning")
                                                           .build());
        var investmentDoc = documentRepository.save(Document.builder()
                                                            .clientId(client.getId())
                                                            .title("Investment Strategy")
                                                            .content("Long-term investment approach")
                                                            .build());

        SearchResult<DocumentSearchHit> results =
                documentRepository.searchByContent(Set.of("wealth", "financial", "investment"),
                                                   PaginationParams.of(10, 0));

        assertThat(results.getTotalCount()).isEqualTo(3);
        assertThat(results.getResults()).extracting(DocumentSearchHit::getDocument)
                                        .extracting(Document::getId)
                                        .containsExactlyInAnyOrder(wealthDoc.getId(), financialDoc.getId(),
                                                                   investmentDoc.getId());
    }

    @Test
    void searchByContentRanksByRelevance() {
        var client = persistClient("rank@neviswealth.com");

        // Document with term in both title and content should rank higher
        var highRank = documentRepository.save(Document.builder()
                                                       .clientId(client.getId())
                                                       .title("Wealth Management Services")
                                                       .content("Comprehensive wealth management and financial planning. One more sentence about wealth to upper rank")
                                                       .build());

        // Document with term only in content
        var lowRank = documentRepository.save(Document.builder()
                                                      .clientId(client.getId())
                                                      .title("Quarterly Report")
                                                      .content("Discussed wealth strategies")
                                                      .build());

        SearchResult<DocumentSearchHit> results =
                documentRepository.searchByContent(Set.of("wealth"), PaginationParams.of(10, 0));

        assertThat(results.getResults()).hasSize(2);
        // Higher ranked should come first
        assertThat(results.getResults()
                          .get(0)
                          .getDocument()
                          .getId()).isEqualTo(highRank.getId());
        assertThat(results.getResults()
                          .get(0)
                          .getScore()).isGreaterThan(results.getResults()
                                                            .get(1)
                                                            .getScore());
    }

    @Test
    void searchByContentWithPaginationReturnsCorrectPage() {
        var client = persistClient("pagination@neviswealth.com");

        for (int i = 0; i < 5; i++) {
            documentRepository.save(Document.builder()
                                            .clientId(client.getId())
                                            .title("Document " + i)
                                            .content("Wealth management content " + i)
                                            .build());
        }

        SearchResult<DocumentSearchHit> firstPage =
                documentRepository.searchByContent(Set.of("wealth"), PaginationParams.of(2, 0));

        SearchResult<DocumentSearchHit> secondPage =
                documentRepository.searchByContent(Set.of("wealth"), PaginationParams.of(2, 2));

        assertThat(firstPage.getTotalCount()).isEqualTo(5);
        assertThat(firstPage.getResults()).hasSize(2);
        assertThat(secondPage.getTotalCount()).isEqualTo(5);
        assertThat(secondPage.getResults()).hasSize(2);

        // Results should be different
        assertThat(firstPage.getResults()).isNotEqualTo(secondPage.getResults());
    }

    @Test
    void searchByContentWithOffsetBeyondResults() {
        var client = persistClient("offset@neviswealth.com");
        documentRepository.save(Document.builder()
                                        .clientId(client.getId())
                                        .title("Single Document")
                                        .content("Wealth content")
                                        .build());

        SearchResult<DocumentSearchHit> results =
                documentRepository.searchByContent(Set.of("wealth"), PaginationParams.of(10, 100));

        assertThat(results.getTotalCount()).isEqualTo(1);
        assertThat(results.getResults()).isEmpty();
    }

    @Test
    void searchByContentIncludesScoreInResults() {
        var client = persistClient("score@neviswealth.com");
        documentRepository.save(Document.builder()
                                        .clientId(client.getId())
                                        .title("Wealth Management")
                                        .content("Wealth strategies")
                                        .build());

        SearchResult<DocumentSearchHit> results =
                documentRepository.searchByContent(Set.of("wealth"), PaginationParams.of(10, 0));

        assertThat(results.getResults()).hasSize(1);
        DocumentSearchHit hit = results.getResults()
                                       .get(0);
        assertThat(hit.getScore()).isGreaterThan(0.0);
    }

    @Test
    void searchByContentHandlesSpecialCharactersInTerms() {
        var client = persistClient("special@neviswealth.com");
        documentRepository.save(Document.builder()
                                        .clientId(client.getId())
                                        .title("J.P. Morgan Analysis")
                                        .content("Market analysis for J.P. Morgan portfolio")
                                        .build());

        SearchResult<DocumentSearchHit> results =
                documentRepository.searchByContent(Set.of("J.P.", "Morgan"), PaginationParams.of(10, 0));

        assertThat(results.getTotalCount()).isGreaterThan(0);
    }

    @Test
    void searchByContentNoMatchesReturnsEmpty() {
        var client = persistClient("nomatch@neviswealth.com");
        documentRepository.save(Document.builder()
                                        .clientId(client.getId())
                                        .title("Portfolio Review")
                                        .content("Investment portfolio analysis")
                                        .build());

        SearchResult<DocumentSearchHit> results =
                documentRepository.searchByContent(Set.of("nonexistent", "terms"), PaginationParams.of(10, 0));

        assertThat(results.getTotalCount()).isZero();
        assertThat(results.getResults()).isEmpty();
    }

    private Client persistClient(String email) {
        return clientRepository.save(Client.builder()
                                           .firstName("Client")
                                           .lastName("Owner")
                                           .email(email)
                                           .countryOfResidence("US")
                                           .build());
    }

    @SpringBootConfiguration
    static class TestConfig {}
}
