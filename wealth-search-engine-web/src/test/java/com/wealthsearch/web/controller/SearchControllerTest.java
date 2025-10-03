package com.wealthsearch.web.controller;

import com.wealthsearch.api.SearchService;
import com.wealthsearch.model.entity.Client;
import com.wealthsearch.model.entity.Document;
import com.wealthsearch.model.entity.search.ClientSearchHit;
import com.wealthsearch.model.entity.search.DocumentSearchHit;
import com.wealthsearch.model.entity.search.PaginationParams;
import com.wealthsearch.model.entity.search.SearchResult;
import com.wealthsearch.model.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Test
    void searchClientsWithValidQuery() throws Exception {
        Client client = createTestClient();
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of(new ClientSearchHit(client, 0.95)))
                .totalCount(1L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("neviswealth"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswealth")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].client.email").value("john.doe@neviswealth.com"))
                .andExpect(jsonPath("$[0].score").value(0.95));
    }

    @Test
    void searchClientsWithPagination() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of(new ClientSearchHit(createTestClient(), 0.95)))
                .totalCount(10L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("neviswealth"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswealth")
                        .param("limit", "5")
                        .param("offset", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "10"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void searchClientsRejectsBlankQuery() throws Exception {
        when(searchService.searchClientsPerCompanyName(eq(""), any(PaginationParams.class)))
                .thenThrow(new BadRequestException("Query should not be empty"));

        mockMvc.perform(get("/search/clients")
                        .param("q", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsRejectsMissingQuery() throws Exception {
        mockMvc.perform(get("/search/clients")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsRejectsLimitTooLarge() throws Exception {
        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswealth")
                        .param("limit", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsRejectsLimitTooSmall() throws Exception {
        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswealth")
                        .param("limit", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsRejectsNegativeOffset() throws Exception {
        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswealth")
                        .param("offset", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsUsesDefaultPagination() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"));
    }

    @Test
    void searchClientsReturnsEmptyResults() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("nonexistent"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchDocumentsWithValidQuery() throws Exception {
        Document document = createTestDocument();
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of(new DocumentSearchHit(document, 0.87)))
                .totalCount(1L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("wealth management"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "wealth management")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.title").value("Test Document"))
                .andExpect(jsonPath("$[0].score").value(0.87));
    }

    @Test
    void searchDocumentsWithPagination() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of(new DocumentSearchHit(createTestDocument(), 0.87)))
                .totalCount(15L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("wealth"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "wealth")
                        .param("limit", "10")
                        .param("offset", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "15"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void searchDocumentsRejectsMissingQuery() throws Exception {
        mockMvc.perform(get("/search/documents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocumentsRejectsBlankQuery() throws Exception {
        when(searchService.searchDocumentsBySimilarTerms(eq(""), any(PaginationParams.class)))
                .thenThrow(new BadRequestException("Query should not be empty"));

        mockMvc.perform(get("/search/documents")
                        .param("q", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocumentsRejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/search/documents")
                        .param("q", "wealth")
                        .param("limit", "200")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocumentsRejectsInvalidOffset() throws Exception {
        mockMvc.perform(get("/search/documents")
                        .param("q", "wealth")
                        .param("offset", "-5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocumentsReturnsEmptyResults() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("nonexistent"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchDocumentsWithSpecialCharacters() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of(new DocumentSearchHit(createTestDocument(), 0.9)))
                .totalCount(1L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("J.P. Morgan"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "J.P. Morgan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"));
    }

    @Test
    void searchClientsWithLimit100() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("limit", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchDocumentsWithLimit1() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of(new DocumentSearchHit(createTestDocument(), 0.9)))
                .totalCount(1L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("limit", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
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

    // ============ ADDITIONAL CORNER CASES FOR CLIENTS ============

    @Test
    void searchClientsWithWhitespaceOnlyQuery() throws Exception {
        when(searchService.searchClientsPerCompanyName(eq("   "), any(PaginationParams.class)))
                .thenThrow(new BadRequestException("Query should not be empty"));

        mockMvc.perform(get("/search/clients")
                        .param("q", "   ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsWithVeryLongQuery() throws Exception {
        String longQuery = "a".repeat(1000);
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq(longQuery), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", longQuery)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"));
    }

    @Test
    void searchClientsWithUnicodeCharacters() throws Exception {
        String unicodeQuery = "富豪 財富";
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq(unicodeQuery), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", unicodeQuery)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchClientsWithEmojiInQuery() throws Exception {
        String emojiQuery = "company 🏢 💼";
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq(emojiQuery), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", emojiQuery)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchClientsWithUrlEncodedSpecialCharacters() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of(new ClientSearchHit(createTestClient(), 0.9)))
                .totalCount(1L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("company&name"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "company&name")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchClientsWithSqlInjectionAttempt() throws Exception {
        String sqlInjection = "'; DROP TABLE clients; --";
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq(sqlInjection), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", sqlInjection)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchClientsWithMultipleResultsSameScore() throws Exception {
        Client client1 = createTestClient();
        Client client2 = Client.builder()
                .id(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@neviswealth.com")
                .domainName("neviswealth")
                .countryOfResidence("US")
                .build();

        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of(
                        new ClientSearchHit(client1, 0.95),
                        new ClientSearchHit(client2, 0.95)
                ))
                .totalCount(2L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("neviswealth"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "neviswealth")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].score").value(0.95))
                .andExpect(jsonPath("$[1].score").value(0.95));
    }

    @Test
    void searchClientsWithOffsetBeyondTotalResults() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(5L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                .param("offset", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchClientsWithLargeOffset() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("offset", "1000000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchClientsWithNonIntegerLimit() throws Exception {
        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("limit", "abc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsWithNonIntegerOffset() throws Exception {
        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("offset", "xyz")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsWithDecimalLimit() throws Exception {
        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("limit", "10.5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsWithDecimalOffset() throws Exception {
        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("offset", "5.7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsWithZeroLimit() throws Exception {
        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("limit", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsWithLimitExactlyAtMax() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("limit", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchClientsWithLimitExactlyAtMin() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of(new ClientSearchHit(createTestClient(), 0.9)))
                .totalCount(1L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("limit", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void searchClientsWithOffsetZero() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of(new ClientSearchHit(createTestClient(), 0.9)))
                .totalCount(1L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchClientsReturnsLargeNumberOfResults() throws Exception {
        Client client = createTestClient();
        List<ClientSearchHit> hits = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            hits.add(new ClientSearchHit(client, 0.95 - (i * 0.001)));
        }

        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(hits)
                .totalCount(500L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("limit", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "500"))
                .andExpect(jsonPath("$", hasSize(100)));
    }

    @Test
    void searchClientsWithCaseSensitiveQuery() throws Exception {
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of(new ClientSearchHit(createTestClient(), 0.9)))
                .totalCount(1L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq("NEVISWEALTH"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", "NEVISWEALTH")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchClientsWithQueryContainingNewlines() throws Exception {
        String queryWithNewlines = "nevis\nwealth";
        SearchResult<ClientSearchHit> result = SearchResult.<ClientSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchClientsPerCompanyName(eq(queryWithNewlines), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/clients")
                        .param("q", queryWithNewlines)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ============ ADDITIONAL CORNER CASES FOR DOCUMENTS ============

    @Test
    void searchDocumentsWithWhitespaceOnlyQuery() throws Exception {
        when(searchService.searchDocumentsBySimilarTerms(eq("   "), any(PaginationParams.class)))
                .thenThrow(new BadRequestException("Query should not be empty"));

        mockMvc.perform(get("/search/documents")
                        .param("q", "   ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocumentsWithVeryLongQuery() throws Exception {
        String longQuery = "a".repeat(1000);
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq(longQuery), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", longQuery)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"));
    }

    @Test
    void searchDocumentsWithUnicodeCharacters() throws Exception {
        String unicodeQuery = "財務報告 文件";
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq(unicodeQuery), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", unicodeQuery)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchDocumentsWithEmojiInQuery() throws Exception {
        String emojiQuery = "document 📄 📊";
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq(emojiQuery), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", emojiQuery)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchDocumentsWithMultipleResultsSameScore() throws Exception {
        Document doc1 = createTestDocument();
        Document doc2 = Document.builder()
                .id(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .title("Another Document")
                .content("More content about wealth management")
                .build();

        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of(
                        new DocumentSearchHit(doc1, 0.87),
                        new DocumentSearchHit(doc2, 0.87)
                ))
                .totalCount(2L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("wealth"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "wealth")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].score").value(0.87))
                .andExpect(jsonPath("$[1].score").value(0.87));
    }

    @Test
    void searchDocumentsWithOffsetBeyondTotalResults() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of())
                .totalCount(3L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("offset", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchDocumentsWithLargeOffset() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("offset", "999999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchDocumentsWithNonIntegerLimit() throws Exception {
        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("limit", "abc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocumentsWithNonIntegerOffset() throws Exception {
        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("offset", "xyz")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocumentsWithDecimalLimit() throws Exception {
        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("limit", "15.5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocumentsWithDecimalOffset() throws Exception {
        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("offset", "7.3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocumentsWithZeroLimit() throws Exception {
        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("limit", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocumentsWithLimitExactlyAtMax() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("limit", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchDocumentsWithLimitExactlyAtMin() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of(new DocumentSearchHit(createTestDocument(), 0.9)))
                .totalCount(1L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("limit", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void searchDocumentsWithOffsetZero() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of(new DocumentSearchHit(createTestDocument(), 0.9)))
                .totalCount(1L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("offset", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchDocumentsReturnsLargeNumberOfResults() throws Exception {
        Document document = createTestDocument();
        List<DocumentSearchHit> hits = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            hits.add(new DocumentSearchHit(document, 0.95 - (i * 0.001)));
        }

        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(hits)
                .totalCount(1000L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("limit", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1000"))
                .andExpect(jsonPath("$", hasSize(100)));
    }

    @Test
    void searchDocumentsWithCaseSensitiveQuery() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of(new DocumentSearchHit(createTestDocument(), 0.9)))
                .totalCount(1L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("WEALTH MANAGEMENT"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "WEALTH MANAGEMENT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchDocumentsWithQueryContainingNewlines() throws Exception {
        String queryWithNewlines = "wealth\nmanagement";
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq(queryWithNewlines), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", queryWithNewlines)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchDocumentsWithSqlInjectionAttempt() throws Exception {
        String sqlInjection = "' OR 1=1; --";
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq(sqlInjection), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", sqlInjection)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchDocumentsWithUrlEncodedQuery() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of(new DocumentSearchHit(createTestDocument(), 0.85)))
                .totalCount(1L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("wealth & finance"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "wealth & finance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void searchDocumentsUsesDefaultPaginationWhenNotProvided() throws Exception {
        SearchResult<DocumentSearchHit> result = SearchResult.<DocumentSearchHit>builder()
                .results(List.of())
                .totalCount(0L)
                .build();

        when(searchService.searchDocumentsBySimilarTerms(eq("test"), any(PaginationParams.class)))
                .thenReturn(result);

        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"));
    }

    @Test
    void searchDocumentsWithNegativeLimit() throws Exception {
        mockMvc.perform(get("/search/documents")
                        .param("q", "test")
                        .param("limit", "-10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClientsWithNegativeLimit() throws Exception {
        mockMvc.perform(get("/search/clients")
                        .param("q", "test")
                        .param("limit", "-5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
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
