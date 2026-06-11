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
public class NotificationEvent {

    private String eventId;
    private Long receiverUserId;
    private Long senderUserId;
    private String type;
    private Long postId;
    private Long commentId;
    private String content;
    private LocalDateTime createdAt;

    public static NotificationEvent create(Long receiverUserId, Long senderUserId, String type,
                                           Long postId, Long commentId, String content) {
        return NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .receiverUserId(receiverUserId)
                .senderUserId(senderUserId)
                .type(type)
                .postId(postId)
                .commentId(commentId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public String messageKey() {
        // Use a stable key so events for the same receiver tend to stay ordered in Kafka.
        return receiverUserId == null ? eventId : String.valueOf(receiverUserId);
    }
}
