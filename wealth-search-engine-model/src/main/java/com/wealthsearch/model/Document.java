package com.wealthsearch.model;

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
@Table(name = "documents")
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
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @Schema(description = "Unique document identifier", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private UUID id;

    @Column(name = "client_id", nullable = false, columnDefinition = "BINARY(16)")
    @NotNull
    @Schema(description = "Identifier of the client owning the document", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID clientId;

    @Column(name = "title", nullable = false, length = 255)
    @NotBlank
    @Size(max = 255)
    @Schema(description = "Document title", example = "KYC Form", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    @Schema(description = "Raw document content", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Column(name = "created_at")
    @Schema(description = "Timestamp when the document was created")
    private OffsetDateTime createdAt;
}
