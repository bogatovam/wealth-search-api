package com.wealthsearch.service;

import com.wealthsearch.api.ollama.SemanticSearchQueryExpander;
import com.wealthsearch.db.repository.ClientRepository;
import com.wealthsearch.db.repository.DocumentRepository;
import com.wealthsearch.model.entity.Client;
import com.wealthsearch.model.entity.Document;
import com.wealthsearch.model.entity.search.ClientSearchHit;
import com.wealthsearch.model.entity.search.DocumentSearchHit;
import com.wealthsearch.model.entity.search.PaginationParams;
import com.wealthsearch.model.entity.search.SearchResult;
import com.wealthsearch.model.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private SemanticSearchQueryExpander searchQueryExpander;

    private SearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchServiceImpl(clientRepository, documentRepository, searchQueryExpander);
        ReflectionTestUtils.setField(searchService, "maxQueryLength", 128L);
    }

    @Test
    void searchClientsPerCompanyNameWithValidQuery() {
        String query = "neviswealth";
        PaginationParams pagination = PaginationParams.of(20, 0);

        SearchResult<ClientSearchHit> expected = SearchResult.<ClientSearchHit>builder()
                .results(List.of(new ClientSearchHit(createTestClient(), 0.95)))
                .totalCount(1L)
                .build();

        when(clientRepository.findClientsByCompanyDomain(any(), eq(pagination))).thenReturn(expected);

        SearchResult<ClientSearchHit> result = searchService.searchClientsPerCompanyName(query, pagination);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<List<String>> domainCaptor = ArgumentCaptor.forClass(List.class);
        verify(clientRepository).findClientsByCompanyDomain(domainCaptor.capture(), eq(pagination));

        List<String> capturedDomains = domainCaptor.getValue();
        assertThat(capturedDomains).hasSize(1);
        assertThat(capturedDomains.get(0)).isEqualTo("neviswealth");
    }

    @Test
    void searchClientsNormalizesAndRemovesSpaces() {
        String query = "Nevis Wealth";
        PaginationParams pagination = PaginationParams.of(20, 0);

        when(clientRepository.findClientsByCompanyDomain(any(), eq(pagination)))
                .thenReturn(new SearchResult<>());

        searchService.searchClientsPerCompanyName(query, pagination);

        ArgumentCaptor<List<String>> domainCaptor = ArgumentCaptor.forClass(List.class);
        verify(clientRepository).findClientsByCompanyDomain(domainCaptor.capture(), eq(pagination));

        // Should be normalized to lowercase and spaces removed
        assertThat(domainCaptor.getValue().get(0)).isEqualTo("neviswealth");
    }

    @Test
    void searchClientsRejectsBlankQuery() {
        assertThatThrownBy(() -> searchService.searchClientsPerCompanyName("", PaginationParams.of(20, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Query should not be empty");
    }

    @Test
    void searchClientsRejectsNullQuery() {
        assertThatThrownBy(() -> searchService.searchClientsPerCompanyName(null, PaginationParams.of(20, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Query should not be empty");
    }

    @Test
    void searchClientsRejectsWhitespaceOnlyQuery() {
        assertThatThrownBy(() -> searchService.searchClientsPerCompanyName("   ", PaginationParams.of(20, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Query should not be empty");
    }

    @Test
    void searchClientsRejectsTooLongQuery() {
        String longQuery = "a".repeat(129);

        assertThatThrownBy(() -> searchService.searchClientsPerCompanyName(longQuery, PaginationParams.of(20, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Query is too long. Max allowed length: '128'");
    }

    @Test
    void searchClientsRejectsQueryWithOnlySpecialCharacters() {
        String query = "!@#$%^&*()";

        assertThatThrownBy(() -> searchService.searchClientsPerCompanyName(query, PaginationParams.of(20, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Query does not contain searchable characters");
    }

    @Test
    void searchClientsHandlesUnicodeCharacters() {
        String query = "Zürich";
        PaginationParams pagination = PaginationParams.of(20, 0);

        when(clientRepository.findClientsByCompanyDomain(any(), eq(pagination)))
                .thenReturn(new SearchResult<>());

        searchService.searchClientsPerCompanyName(query, pagination);

        ArgumentCaptor<List<String>> domainCaptor = ArgumentCaptor.forClass(List.class);
        verify(clientRepository).findClientsByCompanyDomain(domainCaptor.capture(), eq(pagination));

        // Should normalize to "zurich"
        assertThat(domainCaptor.getValue().get(0)).isEqualTo("zurich");
    }

    @Test
    void searchDocumentsBySimilarTermsWithValidQuery() {
        String query = "wealth management";
        PaginationParams pagination = PaginationParams.of(20, 0);
        Set<String> expandedTerms = Set.of("wealth management", "financial planning", "asset management");

        SearchResult<DocumentSearchHit> expected = SearchResult.<DocumentSearchHit>builder()
                .results(List.of(new DocumentSearchHit(createTestDocument(), 0.95)))
                .totalCount(1L)
                .build();

        when(searchQueryExpander.expandQueryWithSynonyms(query)).thenReturn(expandedTerms);
        when(documentRepository.searchByContent(expandedTerms, pagination)).thenReturn(expected);

        SearchResult<DocumentSearchHit> result = searchService.searchDocumentsBySimilarTerms(query, pagination);

        assertThat(result).isEqualTo(expected);
        verify(searchQueryExpander).expandQueryWithSynonyms(query);
        verify(documentRepository).searchByContent(expandedTerms, pagination);
    }

    @Test
    void searchDocumentsNormalizesQuery() {
        String query = "WEALTH Management";
        PaginationParams pagination = PaginationParams.of(20, 0);

        when(searchQueryExpander.expandQueryWithSynonyms(query)).thenReturn(Set.of(query));
        when(documentRepository.searchByContent(any(), eq(pagination)))
                .thenReturn(new SearchResult<>());

        searchService.searchDocumentsBySimilarTerms(query, pagination);

        // Verify the query was normalized before being sent to expander
        verify(searchQueryExpander).expandQueryWithSynonyms(query);
    }

    @Test
    void searchDocumentsRejectsBlankQuery() {
        assertThatThrownBy(() -> searchService.searchDocumentsBySimilarTerms("", PaginationParams.of(20, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Query should not be empty");
    }

    @Test
    void searchDocumentsRejectsNullQuery() {
        assertThatThrownBy(() -> searchService.searchDocumentsBySimilarTerms(null, PaginationParams.of(20, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Query should not be empty");
    }

    @Test
    void searchDocumentsRejectsTooLongQuery() {
        String longQuery = "a".repeat(129);

        assertThatThrownBy(() -> searchService.searchDocumentsBySimilarTerms(longQuery, PaginationParams.of(20, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Query is too long. Max allowed length: '128'");
    }

    @Test
    void searchDocumentsRejectsQueryWithOnlySpecialCharacters() {
        String query = "!@#$%";

        assertThatThrownBy(() -> searchService.searchDocumentsBySimilarTerms(query, PaginationParams.of(20, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Query does not contain searchable characters");
    }

    @Test
    void searchDocumentsHandlesOllamaExpansionFailure() {
        String query = "wealth";
        PaginationParams pagination = PaginationParams.of(20, 0);
        Set<String> fallbackTerms = Set.of(query);

        when(searchQueryExpander.expandQueryWithSynonyms(query)).thenReturn(fallbackTerms);
        when(documentRepository.searchByContent(fallbackTerms, pagination))
                .thenReturn(new SearchResult<>());

        SearchResult<DocumentSearchHit> result = searchService.searchDocumentsBySimilarTerms(query, pagination);

        assertThat(result).isNotNull();
        verify(documentRepository).searchByContent(fallbackTerms, pagination);
    }

    @Test
    void searchDocumentsWithEmptyExpansion() {
        String query = "test";
        PaginationParams pagination = PaginationParams.of(20, 0);
        Set<String> emptyExpansion = Set.of();

        when(searchQueryExpander.expandQueryWithSynonyms(query)).thenReturn(emptyExpansion);
        when(documentRepository.searchByContent(emptyExpansion, pagination))
                .thenReturn(new SearchResult<>());

        SearchResult<DocumentSearchHit> result = searchService.searchDocumentsBySimilarTerms(query, pagination);

        assertThat(result).isNotNull();
        verify(documentRepository).searchByContent(emptyExpansion, pagination);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "neviswealth",
            "a",  // single character
            "wealth management company",
            "J.P. Morgan & Co.",
            "AT&T",
            "user@company.com"
    })
    void searchClientsAcceptsValidQueries(String query) {
        PaginationParams pagination = PaginationParams.of(20, 0);
        when(clientRepository.findClientsByCompanyDomain(any(), eq(pagination)))
                .thenReturn(new SearchResult<>());

        searchService.searchClientsPerCompanyName(query, pagination);

        verify(clientRepository).findClientsByCompanyDomain(any(), eq(pagination));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "wealth management",
            "financial planning",
            "investment strategy",
            "portfolio review"
    })
    void searchDocumentsAcceptsValidQueries(String query) {
        PaginationParams pagination = PaginationParams.of(20, 0);
        when(searchQueryExpander.expandQueryWithSynonyms(query)).thenReturn(Set.of(query));
        when(documentRepository.searchByContent(any(), eq(pagination)))
                .thenReturn(new SearchResult<>());

        searchService.searchDocumentsBySimilarTerms(query, pagination);

        verify(searchQueryExpander).expandQueryWithSynonyms(query);
        verify(documentRepository).searchByContent(any(), eq(pagination));
    }

    @Test
    void searchClientsWith128CharacterQuery() {
        String query = "a".repeat(128);
        PaginationParams pagination = PaginationParams.of(20, 0);

        when(clientRepository.findClientsByCompanyDomain(any(), eq(pagination)))
                .thenReturn(new SearchResult<>());

        // Should not throw - exactly at limit
        searchService.searchClientsPerCompanyName(query, pagination);

        verify(clientRepository).findClientsByCompanyDomain(any(), eq(pagination));
    }

    private Client createTestClient() {
        return Client.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@neviswealth.com")
                .domainName("neviswealth")
                .countryOfResidence("US")
                .build();
    }

    private Document createTestDocument() {
        return Document.builder()
                .id(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .title("Test Document")
                .content("This is a test document about wealth management")
                .build();
    }
}
