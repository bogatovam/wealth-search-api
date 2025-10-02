package com.wealthsearch.model;

import static com.wealthsearch.model.SchemaConstants.Clients;
import static com.wealthsearch.model.SchemaConstants.ColumnDefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

// fixme mapper for validation
@Entity
@Table(name = Clients.TABLE)
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
@Schema(name = "Client", description = "Client profile stored in the platform")
public class Client {

    @Id
    @Column(name = Clients.COLUMN_ID, columnDefinition = ColumnDefinition.UUID)
    @Schema(description = "Unique client identifier", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private UUID id;

    @Column(name = Clients.COLUMN_FIRST_NAME, nullable = false, length = 128)
    @NotBlank
    @Size(max = 128)
    @Schema(description = "Client first name", example = "Ivan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @Column(name = Clients.COLUMN_LAST_NAME, nullable = false, length = 128)
    @NotBlank
    @Size(max = 128)
    @Schema(description = "Client last name", example = "Ivanov", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Column(name = Clients.COLUMN_EMAIL, nullable = false, unique = true, length = 320)
    @NotBlank
    @Email
    @Size(max = 320)
    @Schema(description = "Primary client email", example = "Ivan.Ivanov@neviswealth.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Column(name = Clients.COLUMN_COUNTRY_OF_RESIDENCE, length = 2)
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[A-Z]{2}", message = "Country code must be ISO-3166 alpha-2")
    @Schema(description = "ISO country code where the client resides", example = "CH")
    private String countryOfResidence;

    @Column(name = Clients.COLUMN_CREATED_AT, nullable = false, columnDefinition = ColumnDefinition.TIMESTAMP_WITH_TIME_ZONE, updatable = false)
    @Schema(description = "Timestamp when the client record was created", accessMode = Schema.AccessMode.READ_ONLY)
    private OffsetDateTime createdAt;
}
