package com.wealthsearch.web.controller;

import com.wealthsearch.api.DocumentService;
import com.wealthsearch.model.Document;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients/{clientId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<Document> createDocument(
        @PathVariable("clientId") UUID clientId,
        @Valid @RequestBody Document document
    ) {
        Document documentForClient = document.toBuilder()
            .clientId(clientId)
            .build();

        Document created = documentService.createDocument(documentForClient);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/clients/" + clientId + "/documents/" + created.getId());
        return new ResponseEntity<>(created, headers, HttpStatus.CREATED);
    }
}
