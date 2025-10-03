package com.wealthsearch.model;


import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DocumentSummaryProcessItem {

    private UUID documentId;

    private String status;

    private String summary;

    private OffsetDateTime createdAt;

    private OffsetDateTime completedAt;
}
