package com.forum.server.messaging;

import com.forum.server.messaging.event.SearchIndexEvent;
import com.forum.server.service.SearchIndexWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexEventPublisher {

    private final KafkaTemplate<String, SearchIndexEvent> kafkaTemplate;
    private final SearchIndexWriter searchIndexWriter;

    @Value("${forum.kafka.topics.search-index:forum.search-index.events}")
    private String searchIndexTopic;

    public void publishSync(Long postId) {
        publishAfterCommit(SearchIndexEvent.sync(postId));
    }

    public void publishDelete(Long postId) {
        publishAfterCommit(SearchIndexEvent.delete(postId));
    }

    private void publishAfterCommit(SearchIndexEvent event) {
        if (event.getPostId() == null) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendAsync(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Search index should reflect committed database state, not data that may still roll back.
                sendAsync(event);
            }
        });
    }

    private void sendAsync(SearchIndexEvent event) {
        // Index sync is a side effect, so it should not hold the API response open when Kafka is slow or down.
        CompletableFuture.runAsync(() -> sendToKafka(event));
    }

    private void sendToKafka(SearchIndexEvent event) {
        try {
            kafkaTemplate.send(searchIndexTopic, event.messageKey(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Search index Kafka publish failed, fallback to direct index write. eventId={}", event.getEventId(), ex);
                            fallbackToDirectIndex(event);
                        }
                    });
        } catch (Exception ex) {
            log.warn("Search index Kafka publish failed before send, fallback to direct index write. eventId={}", event.getEventId(), ex);
            fallbackToDirectIndex(event);
        }
    }

    private void fallbackToDirectIndex(SearchIndexEvent event) {
        // This keeps local development usable before Kafka is installed.
        if (SearchIndexEvent.ACTION_DELETE.equals(event.getAction())) {
            searchIndexWriter.deletePostIndex(event.getPostId());
        } else {
            searchIndexWriter.syncPostIndex(event.getPostId());
        }
    }
}
