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
public class SearchIndexEvent {

    public static final String ACTION_SYNC = "sync";
    public static final String ACTION_DELETE = "delete";

    private String eventId;
    private String action;
    private Long postId;
    private LocalDateTime createdAt;

    public static SearchIndexEvent sync(Long postId) {
        return create(ACTION_SYNC, postId);
    }

    public static SearchIndexEvent delete(Long postId) {
        return create(ACTION_DELETE, postId);
    }

    private static SearchIndexEvent create(String action, Long postId) {
        return SearchIndexEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .action(action)
                .postId(postId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public String messageKey() {
        // Ordering by postId makes create/update/delete events for the same post enter the same Kafka partition.
        return postId == null ? eventId : String.valueOf(postId);
    }
}
