package com.forum.server.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCacheInvalidationEvent {

    public static final String ACTION_EVICT_DETAIL = "evict_detail";
    public static final String ACTION_EVICT_ALL = "evict_all";

    private String eventId;
    private String action;
    private Long postId;
    private LocalDateTime createdAt;

    public static PostCacheInvalidationEvent evictDetail(Long postId) {
        return create(ACTION_EVICT_DETAIL, postId);
    }

    public static PostCacheInvalidationEvent evictAll(Long postId) {
        return create(ACTION_EVICT_ALL, postId);
    }

    private static PostCacheInvalidationEvent create(String action, Long postId) {
        return PostCacheInvalidationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .action(action)
                .postId(postId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public String messageKey() {
        return postId == null ? eventId : String.valueOf(postId);
    }
}
