package com.forum.server.messaging;

import com.forum.server.messaging.event.PostCacheInvalidationEvent;
import com.forum.server.service.cache.PostDetailCacheService;
import com.forum.server.service.cache.PostInteractionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCacheInvalidationEventConsumer {

    private final PostDetailCacheService postDetailCacheService;
    private final PostInteractionCacheService postInteractionCacheService;

    @KafkaListener(
            topics = "${forum.kafka.topics.post-cache-invalidation:forum.post-cache-invalidation.events}",
            groupId = "${spring.kafka.consumer.group-id:forum-notification-consumer}"
    )
    public void consume(PostCacheInvalidationEvent event) {
        if (event == null || event.getPostId() == null) {
            return;
        }

        evict(event);
    }

    public void evict(PostCacheInvalidationEvent event) {
        Long postId = event.getPostId();
        postDetailCacheService.evict(postId);

        if (PostCacheInvalidationEvent.ACTION_EVICT_ALL.equals(event.getAction())) {
            postInteractionCacheService.evictPostLike(postId);
            postInteractionCacheService.evictPostFavorite(postId);
        }
    }
}
