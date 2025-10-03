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

    private Document createTestDocument() {
        return Document.builder()
                .id(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .title("Test Document")
                .content("This is a test document about wealth management")
                .build();
    }
}
