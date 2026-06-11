package com.forum.server.messaging;

import com.forum.server.messaging.event.NotificationEvent;
import com.forum.server.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private static final String IDEMPOTENT_KEY_PREFIX = "forum:notification:event:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(1);

    private final NotificationService notificationService;
    private final StringRedisTemplate stringRedisTemplate;

    @KafkaListener(
            topics = "${forum.kafka.topics.notification:forum.notification.events}",
            groupId = "${spring.kafka.consumer.group-id:forum-notification-consumer}"
    )
    public void consume(NotificationEvent event) {
        if (event == null || event.getEventId() == null) {
            return;
        }

        String idempotentKey = IDEMPOTENT_KEY_PREFIX + event.getEventId();
        Boolean firstHandle = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL);
        if (!Boolean.TRUE.equals(firstHandle)) {
            // Kafka may redeliver messages after retry/rebalance; the Redis key prevents duplicate notifications.
            return;
        }

        try {
            notificationService.createNotification(
                    event.getReceiverUserId(),
                    event.getSenderUserId(),
                    event.getType(),
                    event.getPostId(),
                    event.getCommentId(),
                    event.getContent()
            );
        } catch (Exception ex) {
            stringRedisTemplate.delete(idempotentKey);
            log.warn("Notification event consume failed, idempotent key removed for retry. eventId={}", event.getEventId(), ex);
            throw ex;
        }
    }
}
