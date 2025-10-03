# Wealth Search Engine

A semantic search engine for wealth management documents and client data, powered by PostgreSQL full-text search and Ollama AI for query expansion.

## Features

- **Client Search**: Fuzzy search for clients by company domain
- **Document Search**: Semantic search across document titles and content
- **Ollama Integration**: AI-powered query expansion with synonyms and related terms
- **Full-Text Search**: PostgreSQL tsvector and trigram similarity
- **Resilience**: Circuit breaker and retry patterns for Ollama integration
- **Caching**: Query expansion results cached for performance

## Search Execution Examples

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

**Multiple Company Search**
```bash
GET /search/clients?q=neviswealth
GET /search/clients?q=wealthbridge
```
Returns clients ranked by similarity score (0.0 to 1.0)

**With Pagination**
```bash
GET /search/clients?q=neviswealth&limit=10&offset=0   # First page
GET /search/clients?q=neviswealth&limit=10&offset=10  # Second page
```

### 2. Document Search with Semantic Expansion

**Basic Search**
```bash
GET /search/documents?q=wealth&limit=20&offset=0
```
Searches in both document title and content fields

**Semantic Expansion via Ollama**
```bash
GET /search/documents?q=wealth management
```
Ollama expands the query with:
- **Synonyms**: `asset management`, `portfolio management`
- **Related**: `financial planning`, `investment strategy`
- **Narrower**: `401k management`, `estate planning`

**Complex Queries**
```bash
GET /search/documents?q=financial planning
GET /search/documents?q=investment strategy
GET /search/documents?q=retirement planning
```

**Special Characters**
```bash
GET /search/documents?q=J.P. Morgan
```
Handles dots, spaces, and special characters correctly

### 3. Search Result Structure

**Client Search Response**
```json
[
  {
    "client": {
      "id": "uuid",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@neviswealth.com",
      "domainName": "neviswealth",
      "countryOfResidence": "US",
      "createdAt": "2025-10-03T12:00:00Z"
    },
    "score": 0.95
  }
]
```
Header: `X-Total-Count: 1`

**Document Search Response**
```json
[
  {
    "document": {
      "id": "uuid",
      "clientId": "uuid",
      "title": "Wealth Management Strategy",
      "content": "Comprehensive wealth management plan...",
      "createdAt": "2025-10-03T12:00:00Z"
    },
    "score": 0.87
  }
]
```
Header: `X-Total-Count: 15`

### 4. Query Normalization

All queries are automatically normalized:
- **Lowercased**: `NEVISWEALTH` � `neviswealth`
- **Unicode normalized**: `caf�` � `cafe`, `Z�rich` � `zurich`
- **Special chars removed**: Only `@`, `.`, `_`, `'`, `&`, `-` retained
- **Whitespace collapsed**: `multiple   spaces` � `multiple spaces`
- **Trimmed**: `  query  ` � `query`

### 5. Validation Rules

**Query Length**
- Maximum: 128 characters
- Empty queries rejected

**Pagination**
- Limit: 1-100 (default: 20)
- Offset: e 0 (default: 0)

**Error Example**
```bash
GET /search/clients?q=&limit=20
# Returns: 400 Bad Request
# {"errors": [{"message": "Query should not be empty"}]}
```

### 6. Advanced Search Features

**Ranking**
- Documents with matches in both title AND content rank higher
- Clients ranked by trigram + word similarity scores
- Results ordered by score DESC, then createdAt DESC

**Fuzzy Matching (Clients Only)**
Uses PostgreSQL pg_trgm extension:
- Trigram similarity: `similarity(domain_name, 'query')`
- Word similarity: `word_similarity('query', domain_name)`
- Score = MAX(trigram, word_similarity)

**Full-Text Search (Documents Only)**
Uses PostgreSQL tsvector:
- `websearch_to_tsquery` for natural query parsing
- `ts_rank_cd` for relevance ranking
- Searches generated tsvector column for performance

### 7. Ollama Integration

**Query Expansion Process**
1. User submits query: `wealth`
2. System calls Ollama with prompt template
3. Ollama returns structured JSON:
   ```json
   {
     "synonyms": ["wealth management", "asset management"],
     "related": ["financial planning", "investment"],
     "narrower": ["portfolio management"]
   }
   ```
4. All terms combined with OR logic
5. Full-text search executed with expanded query

**Caching**
- Default cache size: 256 entries
- TTL: 10 minutes
- Cache key: original query string
- Eviction: LRU (Least Recently Used)

**Fallback Behavior**
If Ollama is unavailable:
- Search continues with original query
- No error returned to user
- System logs the failure

### 8. Performance Tips

1. **Use specific queries** - `neviswealth` better than `nevis`
2. **Leverage pagination** - Don't fetch all results at once
3. **Monitor X-Total-Count** - Shows total matches without fetching all
4. **Cache-friendly queries** - Repeated queries benefit from Ollama cache

### 9. Example Use Cases

**Find all clients from a company**
```bash
GET /search/clients?q=neviswealth&limit=100&offset=0
```

**Search documents about specific topic**
```bash
GET /search/documents?q=estate planning&limit=20&offset=0
```

**Paginate through large result sets**
```bash
GET /search/documents?q=investment&limit=50&offset=0
GET /search/documents?q=investment&limit=50&offset=50
GET /search/documents?q=investment&limit=50&offset=100
```

**Fuzzy search for misspelled names**
```bash
GET /search/clients?q=nevsis  # Still finds "neviswealth"
```

## Technical Stack

- **Framework**: Spring Boot 3.3.5
- **Database**: PostgreSQL 15+ with pg_trgm extension
- **ORM**: jOOQ 3.19.10
- **AI**: Spring AI + Ollama
- **Resilience**: Resilience4j (Circuit Breaker, Retry)
- **Testing**: JUnit 5, Mockito, Testcontainers

## Notes

- Search is case-insensitive
- All timestamps in UTC
- Email domains automatically extracted and normalized
- Special characters in queries handled gracefully
- Ollama integration optional (graceful degradation)
