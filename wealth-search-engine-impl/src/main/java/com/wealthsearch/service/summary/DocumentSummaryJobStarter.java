package com.wealthsearch.service.summary;

import com.wealthsearch.model.entity.DocumentSummaryProcessItem;

/**
 * Dispatches summary generation work to an asynchronous worker.
 */
public interface DocumentSummaryJobStarter {

    void start(DocumentSummaryProcessItem processItem);
}
