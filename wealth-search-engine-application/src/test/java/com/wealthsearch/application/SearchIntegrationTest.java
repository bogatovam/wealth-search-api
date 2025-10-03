package com.wealthsearch.application;

import com.wealthsearch.api.ollama.client.OllamaClient;
import com.wealthsearch.db.repository.ClientRepository;
import com.wealthsearch.db.repository.DocumentRepository;
import com.wealthsearch.model.entity.Client;
import com.wealthsearch.model.entity.Document;
import com.wealthsearch.model.ollama.FtsQueryExpandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for search functionality.
 * Tests the complete flow from HTTP request through service layers to database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SearchIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @MockBean
    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() {
        // Setup default Ollama mock response
        FtsQueryExpandResult defaultResponse = new FtsQueryExpandResult();
        defaultResponse.setSynonyms(List.of());
        defaultResponse.setRelated(List.of());
        defaultResponse.setNarrower(List.of());
        when(ollamaClient.generate(any())).thenReturn(defaultResponse);
    }

    @Test
    void endToEndClientSearchFlow() throws Exception {
        // Given: Create test clients in database
        clientRepository.save(Client.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@neviswealth.com")
                .countryOfResidence("US")
                .build());

        clientRepository.save(Client.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@wealthbridge.com")
                .countryOfResidence("UK")
                .build());

        // When: Search via HTTP endpoint
        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswealth")
                        .param("limit", "20")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Verify response
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].client.email").value("john.doe@neviswealth.com"))
                .andExpect(jsonPath("$[0].client.domainName").value("neviswealth"))
                .andExpect(jsonPath("$[0].score").value(greaterThan(0.0)));
    }

    @Test
    void endToEndDocumentSearchFlowWithOllamaExpansion() throws Exception {
        // Given: Create test client and documents
        Client client = clientRepository.save(Client.builder()
                .firstName("Test")
                .lastName("Client")
                .email("test@example.com")
                .countryOfResidence("US")
                .build());

        documentRepository.save(Document.builder()
                .clientId(client.getId())
                .title("Wealth Management Strategy")
                .content("Comprehensive wealth management plan for high-net-worth individuals")
                .build());

        documentRepository.save(Document.builder()
                .clientId(client.getId())
                .title("Investment Portfolio Review")
                .content("Quarterly review of investment portfolio performance")
                .build());

        // Mock Ollama to expand "wealth" query
        FtsQueryExpandResult expandedResponse = new FtsQueryExpandResult();
        expandedResponse.setSynonyms(List.of("wealth management", "asset management"));
        expandedResponse.setRelated(List.of("financial planning", "investment"));
        expandedResponse.setNarrower(List.of("portfolio management"));
        when(ollamaClient.generate(any())).thenReturn(expandedResponse);

        // When: Search documents via HTTP endpoint
        mockMvc.perform(get("/search/documents")
                        .param("q", "wealth")
                        .param("limit", "10")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Verify Ollama expansion is used and results returned
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", greaterThanOrEqualTo("1")))
                .andExpect(jsonPath("$[0].document.title").exists())
                .andExpect(jsonPath("$[0].score").value(greaterThan(0.0)));
    }

    @Test
    void clientSearchWithFuzzyMatching() throws Exception {
        // Given: Client with specific domain
        clientRepository.save(Client.builder()
                .firstName("Fuzzy")
                .lastName("Test")
                .email("user@neviswealth.com")
                .countryOfResidence("US")
                .build());

        // When: Search with typo in domain
        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswelth")  // typo: missing 'a'
                        .param("limit", "20")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should still find the client due to fuzzy matching
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", greaterThan("0")))
                .andExpect(jsonPath("$[0].client.email").value(containsString("neviswealth")));
    }

    @Test
    void documentSearchWhenOllamaFails() throws Exception {
        // Given: Document in database
        Client client = clientRepository.save(Client.builder()
                .firstName("Fallback")
                .lastName("Test")
                .email("fallback@example.com")
                .countryOfResidence("US")
                .build());

        documentRepository.save(Document.builder()
                .clientId(client.getId())
                .title("Test Document")
                .content("Wealth management content for testing")
                .build());

        // Mock Ollama to throw exception (simulating service unavailable)
        when(ollamaClient.generate(any())).thenThrow(new RuntimeException("Ollama unavailable"));

        // When: Search documents - should fallback to original query
        mockMvc.perform(get("/search/documents")
                        .param("q", "wealth")
                        .param("limit", "10")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should still return results using original query as fallback
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void searchWithPaginationAcrossPages() throws Exception {
        // Given: Multiple clients with same domain
        for (int i = 0; i < 5; i++) {
            clientRepository.save(Client.builder()
                    .firstName("User" + i)
                    .lastName("Test")
                    .email("user" + i + "@neviswealth.com")
                    .countryOfResidence("US")
                    .build());
        }

        // When: Request first page
        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswealth")
                        .param("limit", "2")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should get first 2 results
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(jsonPath("$", hasSize(2)));

        // When: Request second page
        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswealth")
                        .param("limit", "2")
                        .param("offset", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should get next 2 results
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void searchReturnsEmptyWhenNoMatches() throws Exception {
        // Given: Database has some clients but none matching query
        clientRepository.save(Client.builder()
                .firstName("Unrelated")
                .lastName("Client")
                .email("user@example.com")
                .countryOfResidence("US")
                .build());

        // When: Search for non-existent domain
        mockMvc.perform(get("/search/clients")
                        .param("q", "nonexistentdomain")
                        .param("limit", "20")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return empty results
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void documentSearchRanksByRelevance() throws Exception {
        // Given: Multiple documents with varying relevance
        Client client = clientRepository.save(Client.builder()
                .firstName("Rank")
                .lastName("Test")
                .email("rank@example.com")
                .countryOfResidence("US")
                .build());

        // Highly relevant - term in both title and content
        documentRepository.save(Document.builder()
                .clientId(client.getId())
                .title("Wealth Management Services")
                .content("Comprehensive wealth management and wealth strategies")
                .build());

        // Less relevant - term only in content
        documentRepository.save(Document.builder()
                .clientId(client.getId())
                .title("Quarterly Report")
                .content("Brief mention of wealth")
                .build());

        // When: Search for "wealth"
        mockMvc.perform(get("/search/documents")
                        .param("q", "wealth")
                        .param("limit", "10")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Higher ranked document should come first
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].score").value(greaterThan(
                        Double.parseDouble("$[1].score"))));
    }

    @Test
    void searchHandlesSpecialCharacters() throws Exception {
        // Given: Client with special characters in email
        clientRepository.save(Client.builder()
                .firstName("Special")
                .lastName("Char")
                .email("user+tag@neviswealth.com")
                .countryOfResidence("US")
                .build());

        // When: Search should normalize and find
        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswealth")
                        .param("limit", "20")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should find the client
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", greaterThan("0")));
    }

    @Test
    void searchValidatesQueryLength() throws Exception {
        // When: Query exceeds maximum length
        String longQuery = "a".repeat(129);

        mockMvc.perform(get("/search/clients")
                        .param("q", longQuery)
                        .param("limit", "20")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchValidatesLimit() throws Exception {
        // When: Limit exceeds maximum
        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("limit", "101")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest());

        // When: Limit is below minimum
        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("limit", "0")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest());
    }
}
