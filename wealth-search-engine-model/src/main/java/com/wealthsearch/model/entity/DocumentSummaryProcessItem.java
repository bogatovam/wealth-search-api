package com.wealthsearch.model.entity;

import static com.wealthsearch.model.SchemaConstants.ColumnDefinition;
import static com.wealthsearch.model.SchemaConstants.DocumentSummaryProcessItems;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = DocumentSummaryProcessItems.TABLE)
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
@Schema(name = "DocumentSummaryProcessItem", description = "Summary generation job for a document")
public class DocumentSummaryProcessItem {

    @Id
    @Column(name = DocumentSummaryProcessItems.COLUMN_ID, columnDefinition = ColumnDefinition.UUID)
    @Schema(description = "Unique process item identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID processItemId;

    @Column(name = DocumentSummaryProcessItems.COLUMN_DOCUMENT_ID, nullable = false, columnDefinition = ColumnDefinition.UUID)
    @Schema(description = "Document identifier the summary belongs to", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = DocumentSummaryProcessItems.COLUMN_STATUS, nullable = false, length = 32)
    @Schema(description = "Processing status", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
    private DocumentSummaryProcessStatus status;

    @Column(name = DocumentSummaryProcessItems.COLUMN_SUMMARY, columnDefinition = ColumnDefinition.TEXT)
    @Schema(description = "Generated summary text", accessMode = Schema.AccessMode.READ_ONLY)
    private String summary;

    @Column(name = DocumentSummaryProcessItems.COLUMN_CREATED_AT, nullable = false, columnDefinition = ColumnDefinition.TIMESTAMP_WITH_TIME_ZONE, updatable = false)
    @Schema(description = "When processing started", accessMode = Schema.AccessMode.READ_ONLY)
    private OffsetDateTime createdAt;

    @Column(name = DocumentSummaryProcessItems.COLUMN_COMPLETED_AT, columnDefinition = ColumnDefinition.TIMESTAMP_WITH_TIME_ZONE)
    @Schema(description = "When processing completed", accessMode = Schema.AccessMode.READ_ONLY)
    private OffsetDateTime completedAt;
}
