package com.wealthsearch.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealthsearch.api.DocumentService;
import com.wealthsearch.model.entity.Document;
import com.wealthsearch.model.entity.DocumentSummaryProcessItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    // ============ CREATE DOCUMENT TESTS - POSITIVE ============

    @Test
    void createDocumentWithValidData() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = createTestDocument(null, null);
        Document createdDocument = createTestDocument(UUID.randomUUID(), clientId);

        when(documentService.createDocument(any(Document.class)))
                .thenReturn(createdDocument);

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(createdDocument.getId().toString()))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.title").value("Test Document"))
                .andExpect(jsonPath("$.content").value("This is test content"));
    }

    @Test
    void createDocumentWithVeryLongContent() throws Exception {
        UUID clientId = UUID.randomUUID();
        String longContent = "a".repeat(10000);
        Document inputDocument = Document.builder()
                .title("Long Document")
                .content(longContent)
                .build();
        Document createdDocument = Document.builder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .title("Long Document")
                .content(longContent)
                .createdAt(OffsetDateTime.now())
                .build();

        when(documentService.createDocument(any(Document.class)))
                .thenReturn(createdDocument);

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value(longContent));
    }

    @Test
    void createDocumentWithSpecialCharactersInTitle() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = Document.builder()
                .title("Döcument with spëcial çharacters & symbols: @#$%^&*()")
                .content("Content")
                .build();
        Document createdDocument = inputDocument.toBuilder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .createdAt(OffsetDateTime.now())
                .build();

        when(documentService.createDocument(any(Document.class)))
                .thenReturn(createdDocument);

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(inputDocument.getTitle()));
    }

    @Test
    void createDocumentWithUnicodeCharacters() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = Document.builder()
                .title("文档 📄 Документ")
                .content("內容 содержание 内容 🎉")
                .build();
        Document createdDocument = inputDocument.toBuilder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .createdAt(OffsetDateTime.now())
                .build();

        when(documentService.createDocument(any(Document.class)))
                .thenReturn(createdDocument);

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(inputDocument.getTitle()))
                .andExpect(jsonPath("$.content").value(inputDocument.getContent()));
    }

    @Test
    void createDocumentWithMaxLengthTitle() throws Exception {
        UUID clientId = UUID.randomUUID();
        String maxTitle = "a".repeat(255);
        Document inputDocument = Document.builder()
                .title(maxTitle)
                .content("Content")
                .build();
        Document createdDocument = inputDocument.toBuilder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .createdAt(OffsetDateTime.now())
                .build();

        when(documentService.createDocument(any(Document.class)))
                .thenReturn(createdDocument);

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(maxTitle));
    }

    // ============ CREATE DOCUMENT TESTS - NEGATIVE ============

    @Test
    void createDocumentWithBlankTitle() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = Document.builder()
                .title("")
                .content("Content")
                .build();

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentWithNullTitle() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = Document.builder()
                .title(null)
                .content("Content")
                .build();

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentWithWhitespaceOnlyTitle() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = Document.builder()
                .title("   ")
                .content("Content")
                .build();

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentWithBlankContent() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = Document.builder()
                .title("Title")
                .content("")
                .build();

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentWithNullContent() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = Document.builder()
                .title("Title")
                .content(null)
                .build();

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentWithWhitespaceOnlyContent() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = Document.builder()
                .title("Title")
                .content("   ")
                .build();

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentWithTitleExceedingMaxLength() throws Exception {
        UUID clientId = UUID.randomUUID();
        String tooLongTitle = "a".repeat(256);
        Document inputDocument = Document.builder()
                .title(tooLongTitle)
                .content("Content")
                .build();

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentWithInvalidClientIdFormat() throws Exception {
        mockMvc.perform(post("/clients/{clientId}/documents", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTestDocument(null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentWithMalformedJson() throws Exception {
        UUID clientId = UUID.randomUUID();

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Test\", \"content\": }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentWithEmptyBody() throws Exception {
        UUID clientId = UUID.randomUUID();

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentWithMissingContentType() throws Exception {
        UUID clientId = UUID.randomUUID();

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .content(objectMapper.writeValueAsString(createTestDocument(null, null))))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ============ GET SUMMARY TESTS - POSITIVE ============

    @Test
    void requestSummaryForExistingDocument() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        DocumentSummaryProcessItem summary = DocumentSummaryProcessItem.builder()
                .processItemId(UUID.randomUUID())
                .documentId(documentId)
                .summary("This is a summary")
                .build();

        when(documentService.generateSummaryForDocument(eq(clientId), eq(documentId)))
                .thenReturn(summary);

        mockMvc.perform(get("/clients/{clientId}/documents/{documentId}", clientId, documentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.summary").value("This is a summary"));
    }

    @Test
    void requestSummaryMultipleTimes() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        DocumentSummaryProcessItem summary = DocumentSummaryProcessItem.builder()
                .processItemId(UUID.randomUUID())
                .documentId(documentId)
                .summary("Summary")
                .build();

        when(documentService.generateSummaryForDocument(eq(clientId), eq(documentId)))
                .thenReturn(summary);

        // Request summary twice
        mockMvc.perform(get("/clients/{clientId}/documents/{documentId}", clientId, documentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/clients/{clientId}/documents/{documentId}", clientId, documentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ============ GET SUMMARY TESTS - NEGATIVE ============

    // Note: NotFoundException tests are commented out as the exception class doesn't exist yet
    // Uncomment these tests when NotFoundException is implemented

    /*
    @Test
    void requestSummaryForNonExistentDocument() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        when(documentService.generateSummaryForDocument(eq(clientId), eq(documentId)))
                .thenThrow(new NotFoundException("Document not found"));

        mockMvc.perform(get("/clients/{clientId}/documents/{documentId}", clientId, documentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestSummaryForNonExistentClient() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        when(documentService.generateSummaryForDocument(eq(clientId), eq(documentId)))
                .thenThrow(new NotFoundException("Client not found"));

        mockMvc.perform(get("/clients/{clientId}/documents/{documentId}", clientId, documentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
    */

    @Test
    void requestSummaryWithInvalidClientIdFormat() throws Exception {
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(get("/clients/{clientId}/documents/{documentId}", "invalid-uuid", documentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestSummaryWithInvalidDocumentIdFormat() throws Exception {
        UUID clientId = UUID.randomUUID();

        mockMvc.perform(get("/clients/{clientId}/documents/{documentId}", clientId, "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestSummaryWithBothInvalidUuids() throws Exception {
        mockMvc.perform(get("/clients/{clientId}/documents/{documentId}", "invalid-client", "invalid-doc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /*
    @Test
    void requestSummaryForDocumentBelongingToDifferentClient() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        when(documentService.generateSummaryForDocument(eq(clientId), eq(documentId)))
                .thenThrow(new NotFoundException("Document does not belong to this client"));

        mockMvc.perform(get("/clients/{clientId}/documents/{documentId}", clientId, documentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
    */

    // ============ EDGE CASES ============

    @Test
    void createDocumentWithClientIdInBodyIsOverriddenByPathVariable() throws Exception {
        UUID clientIdInPath = UUID.randomUUID();
        UUID clientIdInBody = UUID.randomUUID();

        Document inputDocument = Document.builder()
                .clientId(clientIdInBody)
                .title("Test Document")
                .content("Content")
                .build();

        Document createdDocument = inputDocument.toBuilder()
                .id(UUID.randomUUID())
                .clientId(clientIdInPath) // Should use path variable
                .createdAt(OffsetDateTime.now())
                .build();

        when(documentService.createDocument(any(Document.class)))
                .thenReturn(createdDocument);

        mockMvc.perform(post("/clients/{clientId}/documents", clientIdInPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(clientIdInPath.toString()));
    }

    @Test
    void createDocumentWithNewlineCharactersInContent() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = Document.builder()
                .title("Multi-line Document")
                .content("Line 1Line 2\r\nLine 3\rLine 4")
                .build();
        Document createdDocument = inputDocument.toBuilder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .createdAt(OffsetDateTime.now())
                .build();

        when(documentService.createDocument(any(Document.class)))
                .thenReturn(createdDocument);

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value(inputDocument.getContent()));
    }

    @Test
    void createDocumentWithJsonEscapeCharacters() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document inputDocument = Document.builder()
                .title("Document with \"quotes\"")
                .content("Content with \\ backslash and \t tab")
                .build();
        Document createdDocument = inputDocument.toBuilder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .createdAt(OffsetDateTime.now())
                .build();

        when(documentService.createDocument(any(Document.class)))
                .thenReturn(createdDocument);

        mockMvc.perform(post("/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDocument)))
                .andExpect(status().isCreated());
    }

    private Document createTestDocument(UUID id, UUID clientId) {
        return Document.builder()
                .id(id)
                .clientId(clientId)
                .title("Test Document")
                .content("This is test content")
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
