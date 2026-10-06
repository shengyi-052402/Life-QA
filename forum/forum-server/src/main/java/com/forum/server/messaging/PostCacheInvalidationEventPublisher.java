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
//清理postDetail信息
    public void publishEvictDetail(Long postId) {
        publishAfterCommit(PostCacheInvalidationEvent.evictDetail(postId));
    }
//清理缓存信息---包括点赞集,收藏集
    public void publishEvictAll(Long postId) {
        publishAfterCommit(PostCacheInvalidationEvent.evictAll(postId));
    }
//处理事物commit后的清理消息
    private void publishAfterCommit(PostCacheInvalidationEvent event) {
        if (event.getPostId() == null) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fallbackConsumer.evict(event);
            sendAsync(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Evict immediately; Kafka is an additional retry path, not the first eviction.
                fallbackConsumer.evict(event);
                sendAsync(event);
            }
        });
    }
//开启异步线程去发送消息
    private void sendAsync(PostCacheInvalidationEvent event) {
        CompletableFuture.runAsync(() -> sendToKafka(event));
    }
//发送消息,设置了兜底的手动清理缓存
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
