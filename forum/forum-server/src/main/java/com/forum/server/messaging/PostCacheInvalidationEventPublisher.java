package com.forum.server.messaging;

import com.forum.server.messaging.event.PostCacheInvalidationEvent;
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
public class PostCacheInvalidationEventPublisher {

    private final KafkaTemplate<String, PostCacheInvalidationEvent> kafkaTemplate;
    private final PostCacheInvalidationEventConsumer fallbackConsumer;

    @Value("${forum.kafka.topics.post-cache-invalidation:forum.post-cache-invalidation.events}")
    private String postCacheInvalidationTopic;

    public void publishEvictDetail(Long postId) {
        publishAfterCommit(PostCacheInvalidationEvent.evictDetail(postId));
    }

    public void publishEvictAll(Long postId) {
        publishAfterCommit(PostCacheInvalidationEvent.evictAll(postId));
    }

    private void publishAfterCommit(PostCacheInvalidationEvent event) {
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
                sendAsync(event);
            }
        });
    }

    private void sendAsync(PostCacheInvalidationEvent event) {
        CompletableFuture.runAsync(() -> sendToKafka(event));
    }

    private void sendToKafka(PostCacheInvalidationEvent event) {
        try {
            kafkaTemplate.send(postCacheInvalidationTopic, event.messageKey(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Post cache invalidation Kafka publish failed, fallback to direct eviction. eventId={}", event.getEventId(), ex);
                            fallbackConsumer.evict(event);
                        }
                    });
        } catch (Exception ex) {
            log.warn("Post cache invalidation Kafka publish failed before send, fallback to direct eviction. eventId={}", event.getEventId(), ex);
            fallbackConsumer.evict(event);
        }
    }
}
