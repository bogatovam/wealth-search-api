package com.wealthsearch.api;

import com.wealthsearch.model.SearchHit;
import java.util.List;

public interface SearchService {
    List<SearchHit> search(String query);
}
