package com.wealthsearch.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Table(name = "clients")
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
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @Schema(description = "Unique client identifier", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 128)
    @NotBlank
    @Size(max = 128)
    @Schema(description = "Client first name", example = "Ivan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 128)
    @NotBlank
    @Size(max = 128)
    @Schema(description = "Client last name", example = "Ivanov", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    @NotBlank
    @Email
    @Size(max = 320)
    @Schema(description = "Primary client email", example = "Ivan.Ivanov@neviswealth.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Column(name = "country_of_residence", length = 120)
    @Size(max = 120)
    @Schema(description = "Country where the client resides", example = "Switzerland")
    private String countryOfResidence;
}
