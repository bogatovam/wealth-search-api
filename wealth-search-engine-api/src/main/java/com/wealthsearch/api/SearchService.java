package com.wealthsearch.api;

import com.wealthsearch.model.ClientSearchHit;
import java.util.List;

public interface SearchService {

    List<ClientSearchHit> searchClientsPerCompanyName(String query);
}
