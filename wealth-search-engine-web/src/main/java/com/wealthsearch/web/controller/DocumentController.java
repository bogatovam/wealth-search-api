package com.wealthsearch.web.controller;

import com.wealthsearch.api.DocumentService;
import com.wealthsearch.model.entity.Document;
import com.wealthsearch.model.entity.DocumentSummaryProcessItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Documents", description = "Document management and summary operations")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/clients/{clientId}/documents")
    @Operation(
            summary = "Upload a document for a client",
            description = "Creates a new document associated with the specified client",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Document created successfully",
                            content = @Content(schema = @Schema(implementation = Document.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid document data provided"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Client not found"
                    )
            }
    )
    public ResponseEntity<Document> createDocument(
            @Parameter(description = "Client identifier", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("clientId") UUID clientId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Document data to create",
                    required = true,
                    content = @Content(schema = @Schema(implementation = Document.class))
            )
            @Valid @RequestBody Document document) {
        Document documentForClient = document.toBuilder()
                                             .clientId(clientId)
                                             .build();

        Document created = documentService.createDocument(documentForClient);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/documents/{documentId}/summary")
    @Operation(
            summary = "Request document summary generation",
            description = "Initiates or retrieves the status of summary generation for a document",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Summary process item returned",
                            content = @Content(schema = @Schema(implementation = DocumentSummaryProcessItem.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Document not found"
                    )
            }
    )
    public ResponseEntity<DocumentSummaryProcessItem> requestSummary(
            @Parameter(description = "Document identifier", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("documentId") UUID documentId) {

        return new ResponseEntity<>(documentService.generateSummaryForDocument(documentId), HttpStatus.OK);
    }

}
