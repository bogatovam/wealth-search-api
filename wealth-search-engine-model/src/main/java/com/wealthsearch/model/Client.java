package com.wealthsearch.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Client {
    UUID id;
    String firstName;
    String lastName;
    String email;
    String countryOfResidence;
}
