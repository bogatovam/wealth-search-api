package com.wealthsearch.client.ollama;

import lombok.Data;

import java.util.List;

@Data
public class FtsQueryExpandResult {

    private List<String> synonyms;

    private List<String> related;

    private List<String> narrower;
}
