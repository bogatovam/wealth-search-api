package com.wealthsearch.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealthsearch.api.ClientService;
import com.wealthsearch.model.entity.Client;
import com.wealthsearch.model.exception.DuplicateClientEmailException;
import com.wealthsearch.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@Import(GlobalExceptionHandler.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientService clientService;

    // ============ CREATE CLIENT TESTS - POSITIVE ============

    @Test
    void createClientWithValidData() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email("john.doe@neviswealth.com")
                                   .countryOfResidence("US")
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName("neviswealth")
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(createdClient.getId()
                                                              .toString()))
               .andExpect(jsonPath("$.firstName").value("John"))
               .andExpect(jsonPath("$.lastName").value("Doe"))
               .andExpect(jsonPath("$.email").value("john.doe@neviswealth.com"))
               .andExpect(jsonPath("$.countryOfResidence").value("US"));
    }

    @Test
    void createClientWithMinimalRequiredFields() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("Jane")
                                   .lastName("Smith")
                                   .email("jane.smith@example.com")
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName("example")
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.firstName").value("Jane"))
               .andExpect(jsonPath("$.lastName").value("Smith"))
               .andExpect(jsonPath("$.email").value("jane.smith@example.com"));
    }

    @Test
    void createClientWithMaxLengthNames() throws Exception {
        String maxName = "a".repeat(128);
        Client inputClient = Client.builder()
                                   .firstName(maxName)
                                   .lastName(maxName)
                                   .email("test@example.com")
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName("example")
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.firstName").value(maxName))
               .andExpect(jsonPath("$.lastName").value(maxName));
    }

    @Test
    void createClientWithMaxLengthEmail() throws Exception {
        // Max email is 320 chars, let's create one close to that
        String localPart = "a".repeat(64);
        String domain = "b".repeat(243);
        String maxEmail = localPart + "@" + domain + ".com";

        Client inputClient = Client.builder()
                                   .firstName("Test")
                                   .lastName("User")
                                   .email(maxEmail)
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName(domain)
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated());
    }

    @Test
    void createClientWithSpecialCharactersInName() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("Jean-François")
                                   .lastName("O'Brien-Müller")
                                   .email("jf@example.com")
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName("example")
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.firstName").value("Jean-François"))
               .andExpect(jsonPath("$.lastName").value("O'Brien-Müller"));
    }

    @Test
    void createClientWithUnicodeCharactersInName() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("李明")
                                   .lastName("Петров")
                                   .email("user@example.com")
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName("example")
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.firstName").value("李明"))
               .andExpect(jsonPath("$.lastName").value("Петров"));
    }

    @Test
    void createClientWithVariousValidCountryCodes() throws Exception {
        String[] countryCodes = {
            "US",
            "GB",
            "CH",
            "DE",
            "FR",
            "JP",
            "CN",
            "RU"
        };

        for (String countryCode : countryCodes) {
            Client inputClient = Client.builder()
                                       .firstName("Test")
                                       .lastName("User")
                                       .email("test" + countryCode + "@example.com")
                                       .countryOfResidence(countryCode)
                                       .build();

            Client createdClient = inputClient.toBuilder()
                                              .id(UUID.randomUUID())
                                              .domainName("example")
                                              .createdAt(OffsetDateTime.now())
                                              .build();

            when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

            mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(inputClient)))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.countryOfResidence").value(countryCode));
        }
    }

    @Test
    void createClientWithComplexEmailAddress() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("Test")
                                   .lastName("User")
                                   .email("test.user+tag@sub.example.co.uk")
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName("sub.example.co")
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.email").value("test.user+tag@sub.example.co.uk"));
    }

    // ============ CREATE CLIENT TESTS - NEGATIVE (VALIDATION ERRORS)
    // ============

    @Test
    void createClientWithBlankFirstName() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("")
                                   .lastName("Doe")
                                   .email("test@example.com")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithNullFirstName() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName(null)
                                   .lastName("Doe")
                                   .email("test@example.com")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithWhitespaceOnlyFirstName() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("   ")
                                   .lastName("Doe")
                                   .email("test@example.com")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithBlankLastName() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("")
                                   .email("test@example.com")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithNullLastName() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName(null)
                                   .email("test@example.com")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithWhitespaceOnlyLastName() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("   ")
                                   .email("test@example.com")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithBlankEmail() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email("")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithNullEmail() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email(null)
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

//    @Test
    void createClientWithInvalidEmailFormat() throws Exception {
        String[] invalidEmails = {
            "notanemail",
            "missing@domain",
            "@nodomain.com",
            "no-at-sign.com",
            "double@@domain.com",
            "spaces in@email.com",
            "trailing-dot@domain.com.",
            ".leading-dot@domain.com"
        };

        for (String invalidEmail : invalidEmails) {
            Client inputClient = Client.builder()
                                       .firstName("John")
                                       .lastName("Doe")
                                       .email(invalidEmail)
                                       .build();

            mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(inputClient)))
                   .andExpect(status().isBadRequest());
        }
    }

    @Test
    void createClientWithFirstNameExceedingMaxLength() throws Exception {
        String tooLongName = "a".repeat(129);
        Client inputClient = Client.builder()
                                   .firstName(tooLongName)
                                   .lastName("Doe")
                                   .email("test@example.com")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithLastNameExceedingMaxLength() throws Exception {
        String tooLongName = "a".repeat(129);
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName(tooLongName)
                                   .email("test@example.com")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

//    @Test()
    void createClientWithEmailExceedingMaxLength() throws Exception {
        String localPart = "a".repeat(64);
        String domain = "b".repeat(250);
        String tooLongEmail = localPart + "@" + domain + ".com";

        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email(tooLongEmail)
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithInvalidCountryCodeFormat() throws Exception {
        String[] invalidCodes = {
            "U",
            "USA",
            "us",
            "u1",
            "1A",
            "U$",
            ""
        };

        for (String invalidCode : invalidCodes) {
            Client inputClient = Client.builder()
                                       .firstName("John")
                                       .lastName("Doe")
                                       .email("test@example.com")
                                       .countryOfResidence(invalidCode)
                                       .build();

            mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(inputClient)))
                   .andExpect(status().isBadRequest());
        }
    }

    @Test
    void createClientWithLowercaseCountryCode() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email("test@example.com")
                                   .countryOfResidence("us")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithMixedCaseCountryCode() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email("test@example.com")
                                   .countryOfResidence("Us")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isBadRequest());
    }

    // ============ CREATE CLIENT TESTS - NEGATIVE (BUSINESS LOGIC ERRORS)
    // ============

    @Test
    void createClientWithDuplicateEmail() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email("duplicate@example.com")
                                   .build();

        when(clientService.createClient(any(Client.class))).thenThrow(new DuplicateClientEmailException(
                "duplicate@example.com"));

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isConflict());
    }

    // ============ CREATE CLIENT TESTS - MALFORMED REQUESTS ============

    @Test
    void createClientWithMalformedJson() throws Exception {
        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"firstName\": \"John\", \"lastName\": }"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithEmptyBody() throws Exception {
        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(""))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithEmptyJsonObject() throws Exception {
        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createClientWithMissingContentType() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email("test@example.com")
                                   .build();

        mockMvc.perform(post("/clients").content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void createClientWithInvalidContentType() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email("test@example.com")
                                   .build();

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_XML)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isUnsupportedMediaType());
    }

    // ============ EDGE CASES ============

    @Test
    void createClientWithExtraUnknownFields() throws Exception {
        String jsonWithExtraFields = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "test@example.com",
                    "unknownField": "should be ignored",
                    "anotherUnknownField": 123
                }
                """;

        Client createdClient = Client.builder()
                                     .id(UUID.randomUUID())
                                     .firstName("John")
                                     .lastName("Doe")
                                     .email("test@example.com")
                                     .domainName("example")
                                     .createdAt(OffsetDateTime.now())
                                     .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(jsonWithExtraFields))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void createClientWithNumericNames() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("123")
                                   .lastName("456")
                                   .email("test@example.com")
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName("example")
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.firstName").value("123"));
    }

    @Test
    void createClientWithSingleCharacterNames() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("J")
                                   .lastName("D")
                                   .email("jd@example.com")
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName("example")
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.firstName").value("J"))
               .andExpect(jsonPath("$.lastName").value("D"));
    }

    @Test
    void createClientWithEmailContainingPlusSign() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email("john.doe+tag@example.com")
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName("example")
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.email").value("john.doe+tag@example.com"));
    }

    @Test
    void createClientWithNullCountryOfResidence() throws Exception {
        Client inputClient = Client.builder()
                                   .firstName("John")
                                   .lastName("Doe")
                                   .email("test@example.com")
                                   .countryOfResidence(null)
                                   .build();

        Client createdClient = inputClient.toBuilder()
                                          .id(UUID.randomUUID())
                                          .domainName("example")
                                          .createdAt(OffsetDateTime.now())
                                          .build();

        when(clientService.createClient(any(Client.class))).thenReturn(createdClient);

        mockMvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(inputClient)))
               .andExpect(status().isCreated());
    }
}
