package com.wealthsearch.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class SearchHit {
    Client client;
    Document document;
    double score;
}
