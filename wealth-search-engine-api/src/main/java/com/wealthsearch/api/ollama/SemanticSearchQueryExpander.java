package com.wealthsearch.api.ollama;

import java.util.Set;

public interface SemanticSearchQueryExpander {

    Set<String> expandQueryWithSynonyms(String query);
}
