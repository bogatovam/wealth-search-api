package com.wealthsearch.model;

public final class SchemaConstants {

    private SchemaConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final class Clients {
        public static final String TABLE = "clients";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_FIRST_NAME = "first_name";
        public static final String COLUMN_LAST_NAME = "last_name";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_COUNTRY_OF_RESIDENCE = "country_of_residence";
        public static final String COLUMN_DOMAIN_NAME = "domain_name";
        public static final String COLUMN_CREATED_AT = "created_at";

        private Clients() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    public static final class Documents {
        public static final String TABLE = "documents";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_CLIENT_ID = "client_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_CONTENT = "content";
        public static final String COLUMN_CREATED_AT = "created_at";

        private Documents() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    public static final class DocumentSummaryProcessItems {
        public static final String TABLE = "document_summary_process_items";
        public static final String COLUMN_DOCUMENT_ID = "document_id";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_SUMMARY = "summary";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_COMPLETED_AT = "completed_at";

        private DocumentSummaryProcessItems() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    public static final class ColumnDefinition {
        public static final String UUID = "UUID";
        public static final String TEXT = "TEXT";
        public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";

        private ColumnDefinition() {
            throw new UnsupportedOperationException("Utility class");
        }
    }
}
