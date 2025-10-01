package com.wealthsearch.web.mapper;

import com.wealthsearch.model.Client;

public final class ClientMapper {

    private ClientMapper() {
    }

    public static com.wealthsearch.web.openapi.model.Client toApi(Client domainClient) {
        if (domainClient == null) {
            return null;
        }
        var apiClient = new com.wealthsearch.web.openapi.model.Client();
        apiClient.setId(domainClient.getId() != null ? domainClient.getId().toString() : null);
        apiClient.setFirstName(domainClient.getFirstName());
        apiClient.setLastName(domainClient.getLastName());
        apiClient.setEmail(domainClient.getEmail());
        apiClient.setCountryOfResidence(domainClient.getCountryOfResidence());
        return apiClient;
    }
}
