package com.wealthsearch.model.ollama;

import lombok.Data;

import java.util.List;

@Data
public class FtsQueryExpandResult {

    private List<String> synonyms;

    private List<String> related;

    private List<String> narrower;
}
