# Wealth Search Engine

## Quick Links

- Installation notes: `docs/installation-notes.md`
- Swagger UI (running app): `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON (running app): `http://localhost:8080/v3/api-docs`
- Swagger spec (snapshot): `docs/swagger-spec.json`
- 
## Features

### 1. Client Search by Company Domain

**Simple Exact Match**
```bash
GET /search/clients?q=neviswealth&limit=20&offset=0
```
Finds clients with email domains like `@neviswealth.com`

**Fuzzy Match (Handles Typos)**
```bash
GET /search/clients?q=neviswelth&limit=20&offset=0
```
Still finds `neviswealth` clients despite the typo using trigram similarity

#### Implementation Details

Query and Email Normalization + pg_trgm based filtering

#### Examples

```bash
curl --location 'localhost:8080/search/clients?q=Nevis%20Wealth'
```
```json
[
  {
    "client": {
      "id": "174c9581-5abc-432e-9526-5295d6e96330",
      "firstName": "Fedor",
      "lastName": "Fedotov",
      "email": "insights@neviswealth.com",
      "countryOfResidence": "US",
      "createdAt": "2025-10-03T19:39:26.801982+02:00"
    },
    "score": 1.0
  },
  {
    "client": {
      "id": "8e9dd586-6251-4066-8880-b17a45b25828",
      "firstName": "Roman",
      "lastName": "Romanov",
      "email": "insights@wealth-nevis.com",
      "countryOfResidence": "US",
      "createdAt": "2025-10-03T19:33:35.78364+02:00"
    },
    "score": 0.5
  }
]
```


```bash
curl --location 'localhost:8080/search/clients?q=nevisis'
```
```json
[
    {
        "client": {
            "id": "174c9581-5abc-432e-9526-5295d6e96330",
            "firstName": "Fedor",
            "lastName": "Fedotov",
            "email": "insights@neviswealth.com",
            "countryOfResidence": "US",
            "createdAt": "2025-10-03T19:39:26.801982+02:00"
        },
        "score": 0.625
    }
]
```

```bash
curl --location 'localhost:8080/search/clients?q=J.P.%20Morgan'
```
```json
[
  {
    "client": {
      "id": "d4d063e4-e56c-4f15-8478-388d92a103d2",
      "firstName": "Roman",
      "lastName": "Romanov",
      "email": "insights@jpmorgan.com",
      "countryOfResidence": "US",
      "createdAt": "2025-10-03T19:44:00.578447+02:00"
    },
    "score": 1.0
  },
  {
    "client": {
      "id": "ef672f0a-8a6f-4e0b-af73-179c6d7722e0",
      "firstName": "Roman",
      "lastName": "Romanov",
      "email": "insights@j.p.morgan.com",
      "countryOfResidence": "US",
      "createdAt": "2025-10-03T20:03:28.570575+02:00"
    },
    "score": 1.0
  }
]
```

### 2. Document Search with Semantic Expansion

**Basic Search**
```bash
GET /search/documents?q=wealth&limit=20&offset=0
```
Searches in document content fields

**Semantic Expansion via Ollama**
```bash
GET /search/documents?q=wealth management
```
Ollama expands the query with:
- **Synonyms**: `asset management`, `portfolio management`
- **Related**: `financial planning`, `investment strategy`
- **Narrower**: `estate planning`

Handles dots, spaces, and special characters correctly

#### Implementation Details

Query Normalization + Postgres FTS + LLM query expansion

#### Examples

```bash
curl --location 'localhost:8080/search/documents?q=residence%20verification'
```
```json
[
  {
    "document": {
      "id": "55b2c99a-7c18-4d27-bcb9-51ff04f6f7ed",
      "clientId": "06b8658e-816a-4382-a017-024882dec1a5",
      "title": "Passport Scan – Joseph Clarke",
      "content": "passport scan uploaded for Joseph Clarke as part of the identity verification review. Residence registered since 2012 with supporting evidence (health insurance certificate, rental contract).\n\nCompliance context: controls assessed against HKMA AML Guideline. Declared average balance measured at 50.29 with monthly income 7,246.47 AED. All personal identifiers validated against passport and national registry data.\n\nNext steps: confirm address change with postal service, archive notarised copy in secure vault, notify compliance review queue. Client will be notified once verification and archival steps are complete.\n\nReference ID 8-1 | Generated 2025-10-03 19:26:42",
      "createdAt": "2025-10-03T19:27:10.10825+02:00"
    },
    "score": 0.255
  },
  {
    "document": {
      "id": "18055e89-0a7a-4fb2-9f3e-54dbd012cc98",
      "clientId": "3bacad33-1f8e-465e-8a48-0fa56ad806bc",
      "title": "Passport Scan for FATF Travel Rule - Q4 2025 - Caroline Strong",
      "content": "This document summarizes the Passport Scan received for Caroline Strong, resident since 2019 at QA. The scan supports wealth assessment under FATF Travel Rule regulations.  Her financial information includes a mortgage balance of $6.87 SGD and income of SGD 11,159.08. \n\nThe document highlights the need for additional supporting invoices to enhance the accuracy of the assessment. Furthermore, we are conducting FATF Travel Rule compliance checks and refreshing sanctions screening based on Caroline Strong's profile.  We will share the summary with her Relationship Manager for further action.\n\nNext steps include requesting additional supporting documents from Caroline Strong, such as utility bills or bank statements, to validate income claims. We also need to refresh sanctions screening lists to ensure accuracy in compliance with FATF Travel Rule regulations.",
      "createdAt": "2025-10-03T19:28:11.429403+02:00"
    },
    "score": 0.2
  },
  {
    "document": {
      "id": "73b02b35-cc62-4808-898c-bb57723ab3f0",
      "clientId": "d6078ffa-0a78-4c2c-b7da-c09df8a2b899",
      "title": "Income Statement – John Vance",
      "content": "income statement uploaded for John Vance as part of the wealth assessment review. Residence registered since 2022 with supporting evidence (national ID, IBAN).\n\nCompliance context: controls assessed against FATF Travel Rule. Declared card spending measured at 88.90 with monthly income 23,555.99 USD. All personal identifiers validated against passport and national registry data.\n\nNext steps: share summary with relationship manager, recalculate affordability metrics, confirm address change with postal service. Client will be notified once verification and archival steps are complete.\n\nReference ID 1-1 | Generated 2025-10-03 19:23:35",
      "createdAt": "2025-10-03T19:24:14.332003+02:00"
    },
    "score": 0.11
  }
]
```


```bash
curl --location 'localhost:8080/search/documents?q=wealth%20management'
```
```json
[
  {
    "document": {
      "id": "73b02b35-cc62-4808-898c-bb57723ab3f0",
      "clientId": "d6078ffa-0a78-4c2c-b7da-c09df8a2b899",
      "title": "Income Statement – John Vance",
      "content": "income statement uploaded for John Vance as part of the wealth assessment review. Residence registered since 2022 with supporting evidence (national ID, IBAN).\n\nCompliance context: controls assessed against FATF Travel Rule. Declared card spending measured at 88.90 with monthly income 23,555.99 USD. All personal identifiers validated against passport and national registry data.\n\nNext steps: share summary with relationship manager, recalculate affordability metrics, confirm address change with postal service. Client will be notified once verification and archival steps are complete.\n\nReference ID 1-1 | Generated 2025-10-03 19:23:35",
      "createdAt": "2025-10-03T19:24:14.332003+02:00"
    },
    "score": 0.0020408165
  }
]
```

### 3. Document Summary

#### How It Works

- First call creates a process item with status `IN_PROGRESS` and returns it immediately.
- A background job builds a prompt from `prompts/doc-summary.txt` using the document content and calls the local LLM via Ollama (Spring AI).
- On success, the process becomes `COMPLETED` and `summary` is persisted. On error, status is set to `FAILED`.
- Subsequent calls:
    - `IN_PROGRESS` or `COMPLETED`: returns current state.
    - `FAILED`: automatically flips to `IN_PROGRESS` and re-triggers generation.

Example Responses

In progress:
```json
{
  "processItemId": "123e4567-e89b-12d3-a456-426614174000",
  "documentId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "IN_PROGRESS",
  "summary": null,
  "createdAt": "2025-10-03T19:22:10Z",
  "completedAt": null
}
```

Completed:
```json
{
  "processItemId": "123e4567-e89b-12d3-a456-426614174000",
  "documentId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "COMPLETED",
  "summary": "This document outlines ... (one concise paragraph)",
  "createdAt": "2025-10-03T19:22:10Z",
  "completedAt": "2025-10-03T19:23:05Z"
}
```

## Technical Stack

- **Framework**: Spring Boot 3.3.5
- **Database**: PostgreSQL 15+ with pg_trgm extension
- **ORM**: jOOQ 3.19.10
- **AI**: Spring AI + Ollama
- **Resilience**: Resilience4j (Circuit Breaker, Retry)
- **Testing**: JUnit 5, Mockito, Testcontainers

## Assumptions

- Search is case-insensitive
- All timestamps in UTC
- Email domains automatically extracted and normalized
- Special characters in queries handled gracefully
- Ollama integration optional (graceful degradation)
