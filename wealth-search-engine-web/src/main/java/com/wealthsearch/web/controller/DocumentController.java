package com.wealthsearch.web.controller;

import com.wealthsearch.api.CreateDocumentCommand;
import com.wealthsearch.api.DocumentService;
import com.wealthsearch.web.dto.CreateDocumentRequest;
import com.wealthsearch.web.mapper.DocumentMapper;
import jakarta.validation.Valid;
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
    public ResponseEntity<com.wealthsearch.web.openapi.model.Document> createDocument(
        @PathVariable("clientId") UUID clientId,
        @Valid @RequestBody CreateDocumentRequest request
    ) {
        var command = new CreateDocumentCommand(request.title(), request.content());
        var created = documentService.createDocument(clientId, command);
        var responseBody = DocumentMapper.toApi(created);
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/clients/" + clientId + "/documents/" + created.getId());
        return new ResponseEntity<>(responseBody, headers, HttpStatus.CREATED);
    }
}
