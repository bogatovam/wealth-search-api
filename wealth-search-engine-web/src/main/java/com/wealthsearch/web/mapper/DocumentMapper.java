package com.wealthsearch.web.mapper;

import com.wealthsearch.model.Document;

public final class DocumentMapper {

    private DocumentMapper() {
    }

    public static com.wealthsearch.web.openapi.model.Document toApi(Document domainDocument) {
        if (domainDocument == null) {
            return null;
        }
        var apiDocument = new com.wealthsearch.web.openapi.model.Document();
        apiDocument.setId(domainDocument.getId() != null ? domainDocument.getId().toString() : null);
        apiDocument.setClientId(domainDocument.getClientId() != null ? domainDocument.getClientId().toString() : null);
        apiDocument.setTitle(domainDocument.getTitle());
        apiDocument.setContent(domainDocument.getContent());
        apiDocument.setCreatedAt(domainDocument.getCreatedAt());
        return apiDocument;
    }
}
