package com.wealthsearch.web.controller;

import com.wealthsearch.api.ClientService;
import com.wealthsearch.model.entity.Client;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
@Tag(name = "Clients", description = "Client management operations")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @Operation(
            summary = "Create a new client",
            description = "Creates a new client profile in the system",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Client created successfully",
                            content = @Content(schema = @Schema(implementation = Client.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid client data provided"
                    )
            }
    )
    public ResponseEntity<Client> createClient(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Client data to create",
                    required = true,
                    content = @Content(schema = @Schema(implementation = Client.class))
            )
            @Valid @RequestBody Client client) {
        Client created = clientService.createClient(client);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
