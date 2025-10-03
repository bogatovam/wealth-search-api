package com.wealthsearch.web.controller;

import com.wealthsearch.api.DocumentService;
import com.wealthsearch.model.entity.Document;
import com.wealthsearch.model.entity.DocumentSummaryProcessItem;
import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients/{clientId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<Document> createDocument(@PathVariable("clientId") UUID clientId,
            @Valid @RequestBody Document document) {
        Document documentForClient = document.toBuilder()
                                             .clientId(clientId)
                                             .build();

        Document created = documentService.createDocument(documentForClient);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{documentId}/summary")
    public ResponseEntity<DocumentSummaryProcessItem> requestSummary(@PathVariable("clientId") UUID clientId,
            @PathVariable("documentId") UUID documentId) {

        return new ResponseEntity<>(documentService.generateSummaryForDocument(clientId, documentId), HttpStatus.OK);
    }

}
