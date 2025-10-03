package com.wealthsearch.service;

import com.wealthsearch.api.DocumentService;
import com.wealthsearch.db.repository.ClientRepository;
import com.wealthsearch.db.repository.DocumentRepository;
import com.wealthsearch.db.repository.DocumentSummaryProcessItemRepository;
import com.wealthsearch.model.entity.Document;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.wealthsearch.model.entity.DocumentSummaryProcessItem;
import com.wealthsearch.model.entity.DocumentSummaryProcessStatus;
import com.wealthsearch.model.exception.ConflictException;
import com.wealthsearch.model.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;

    private final DocumentSummaryProcessItemRepository summaryProcessItemRepository;

    private final ClientRepository clientRepository;

    private final ExecutorService e = this.prepareExecutorService();

    private final SummaryGenerationService summaryGenerationService;

    private final Map<UUID, Lock> docId2Lock = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public Document createDocument(Document document) {
        Objects.requireNonNull(document, "document must not be null");

        UUID clientId = document.getClientId();
        if (clientId == null) {
            throw new IllegalArgumentException("Client id must be provided");
        }

        clientRepository.findById(clientId)
                        .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        Document toPersist = document.toBuilder()
                                     .id(null)
                                     .clientId(clientId)
                                     .build();

        return documentRepository.save(toPersist);
    }

    @Override
    @Transactional
    public DocumentSummaryProcessItem generateSummaryForDocument(UUID clientId, UUID documentId) {
        Document document = documentRepository.findById(documentId)
                                              .orElseThrow(() -> new NotFoundException(
                                                      "Document with this id not found"));

        Lock lock = docId2Lock.computeIfAbsent(documentId, (d) -> new ReentrantLock());

        try (LockAutoClosable lockAutoClosable = new LockAutoClosable(lock)) {

            lockAutoClosable.tryLock(20);

            var activeGeneration = summaryProcessItemRepository.findById(documentId);

            DocumentSummaryProcessItem documentSummaryProcessItem;
            if (activeGeneration.isPresent()) {
                if (Objects.equals(DocumentSummaryProcessStatus.IN_PROGRESS, activeGeneration.get()
                                                                                             .getStatus())
                        || Objects.equals(DocumentSummaryProcessStatus.COMPLETED, activeGeneration.get()
                                                                                                  .getStatus())) {
                    return activeGeneration.get();
                } else {
                    documentSummaryProcessItem =
                            summaryProcessItemRepository.markStatus(documentId,
                                                                    DocumentSummaryProcessStatus.IN_PROGRESS);
                }
            } else {
                documentSummaryProcessItem = summaryProcessItemRepository.insertEventOrReturnExisting(documentId);
            }

            try {
                e.submit(() -> {
                    try {
                        summaryGenerationService.generateSummaryForDocumentAsync(document, documentSummaryProcessItem);
                    } catch (Exception any) {
                        summaryProcessItemRepository.markStatus(documentSummaryProcessItem.getDocumentId(),
                                                                DocumentSummaryProcessStatus.FAILED);
                        throw any;
                    }
                });
            } catch (Exception any) {
                summaryProcessItemRepository.markStatus(documentId, DocumentSummaryProcessStatus.FAILED);
                throw any;
            }

            return documentSummaryProcessItem;
        } catch (InterruptedException e) {
            throw new ConflictException("Error while try to lock");
        }
    }

    private ExecutorService prepareExecutorService() {
        return new ThreadPoolExecutor(0, 1, 0L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
