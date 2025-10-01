package com.wealthsearch.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
    @JsonProperty("first_name") @NotBlank @Size(max = 128) String firstName,
    @JsonProperty("last_name") @NotBlank @Size(max = 128) String lastName,
    @JsonProperty("email") @NotBlank @Email String email,
    @JsonProperty("countryOfResidence") @Size(max = 120) String countryOfResidence
) {
}
