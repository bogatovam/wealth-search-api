package com.wealthsearch.web.controller;

import com.wealthsearch.api.ClientService;
import com.wealthsearch.api.CreateClientCommand;
import com.wealthsearch.web.dto.CreateClientRequest;
import com.wealthsearch.web.mapper.ClientMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<com.wealthsearch.web.openapi.model.Client> createClient(
            @Valid @RequestBody CreateClientRequest request
    ) {
        var command = new CreateClientCommand(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.countryOfResidence()
        );
        var created = clientService.createClient(command);
        var responseBody = ClientMapper.toApi(created);

        return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
    }
}
