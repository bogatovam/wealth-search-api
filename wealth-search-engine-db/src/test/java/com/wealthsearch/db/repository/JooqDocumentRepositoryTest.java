package com.wealthsearch.db.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wealthsearch.db.config.JooqSettingsConfiguration;
import com.wealthsearch.db.repository.exception.EntityAlreadyExistsException;
import com.wealthsearch.db.repository.support.PostgresContainerSupport;
import com.wealthsearch.model.Client;
import com.wealthsearch.model.Document;
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
@Import({JooqDocumentRepository.class, JooqClientRepository.class, JooqSettingsConfiguration.class})
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
        assertThat(saved.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(documentRepository.findByClientId(client.getId()))
            .extracting(Document::getId)
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
            .build()))
            .isInstanceOf(EntityAlreadyExistsException.class);
    }

    @Test
    void findByClientIdOrdersByMostRecent() {
        var client = persistClient("advisor@neviswealth.com");
        var older = documentRepository.save(Document.builder()
            .clientId(client.getId())
            .title("Initial Strategy")
            .content("Discussed long-term diversification")
            .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2))
            .build());
        var newer = documentRepository.save(Document.builder()
            .clientId(client.getId())
            .title("QBR Summary")
            .content("Reviewed quarterly performance")
            .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
            .build());

        List<Document> results = documentRepository.findByClientId(client.getId());
        assertThat(results)
            .extracting(Document::getId)
            .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void searchByContentMatchesTitleAndBody() {
        var client = persistClient("investor@neviswealth.com");
        var titleMatch = documentRepository.save(Document.builder()
            .clientId(client.getId())
            .title("Nevis Wealth onboarding checklist")
            .content("Documents required for onboarding")
            .build());
        var contentMatch = documentRepository.save(Document.builder()
            .clientId(client.getId())
            .title("Quarterly review")
            .content("Discussed nevis wealth expansion plans")
            .build());
        documentRepository.save(Document.builder()
            .clientId(client.getId())
            .title("Unrelated")
            .content("General market commentary")
            .build());

        List<Document> results = documentRepository.searchByContent("nevis wealth");
        assertThat(results)
            .extracting(Document::getId)
            .containsExactly(contentMatch.getId(), titleMatch.getId());
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
    static class TestConfig {
    }
}
