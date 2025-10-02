package com.wealthsearch.model;

import static com.wealthsearch.model.SchemaConstants.ColumnDefinition;
import static com.wealthsearch.model.SchemaConstants.Documents;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Table(name = Documents.TABLE)
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
@Schema(name = "Document", description = "Document uploaded for a client")
public class Document {

    @Id
    @Column(name = Documents.COLUMN_ID, columnDefinition = ColumnDefinition.UUID)
    @Schema(description = "Unique document identifier", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private UUID id;

    @Column(name = Documents.COLUMN_CLIENT_ID, nullable = false, columnDefinition = ColumnDefinition.UUID)
    @NotNull
    @Schema(description = "Identifier of the client owning the document", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID clientId;

    @Column(name = Documents.COLUMN_TITLE, nullable = false, length = 255)
    @NotBlank
    @Size(max = 255)
    @Schema(description = "Document title", example = "KYC Form", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Column(name = Documents.COLUMN_CONTENT, nullable = false, columnDefinition = ColumnDefinition.TEXT)
    @NotBlank
    @Schema(description = "Raw document content", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Column(name = Documents.COLUMN_CREATED_AT, nullable = false, columnDefinition = ColumnDefinition.TIMESTAMP_WITH_TIME_ZONE)
    @Schema(description = "Timestamp when the document was created", accessMode = Schema.AccessMode.READ_ONLY)
    private OffsetDateTime createdAt;
}
