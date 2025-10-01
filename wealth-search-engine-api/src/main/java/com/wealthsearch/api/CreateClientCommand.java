package com.wealthsearch.api;

public record CreateClientCommand(String firstName, String lastName, String email, String countryOfResidence) {
}
