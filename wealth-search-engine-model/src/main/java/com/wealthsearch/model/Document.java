package com.wealthsearch.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Document {
    UUID id;
    UUID clientId;
    String title;
    String content;
    OffsetDateTime createdAt;
}
