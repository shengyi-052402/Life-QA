package com.forum.server.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forum.pojo.entity.NotificationOutboxEvent;
import com.forum.server.mapper.NotificationOutboxEventMapper;
import com.forum.server.messaging.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Delivers persisted interaction events after their source transaction commits. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxDispatcher {
    private static final int MAX_RETRIES = 5;

    private final NotificationOutboxEventMapper outboxMapper;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${forum.kafka.topics.notification:forum.notification.events}")
    private String notificationTopic;

    /**
     * 读取outbox中信息
     */
    @Scheduled(fixedDelayString = "${forum.distribution.outbox-dispatch-delay-ms:5000}")
    public void dispatchPending() {
        LocalDateTime now = LocalDateTime.now();
        List<NotificationOutboxEvent> events = outboxMapper.selectList(new LambdaQueryWrapper<NotificationOutboxEvent>()
                .and(query -> query.eq(NotificationOutboxEvent::getStatus, 0)
                                .le(NotificationOutboxEvent::getNextRetryAt, now)
                        .or(stale -> stale.eq(NotificationOutboxEvent::getStatus, 1)
                                .lt(NotificationOutboxEvent::getLockedUntil, now)))
                .orderByAsc(NotificationOutboxEvent::getId)
                .last("LIMIT 100"));
        for (NotificationOutboxEvent event : events) {
            String token = UUID.randomUUID().toString();
            if (outboxMapper.claim(event.getId(), token, now.plusMinutes(2)) == 1) dispatch(event, token);
        }
    }

    public void retry(Long id) {
        outboxMapper.resetFailed(id);
    }

    /**
     * 发送kafka消息
     *
     * @param outbox
     * @param token
     */
    private void dispatch(NotificationOutboxEvent outbox, String token) {
        NotificationEvent event = NotificationEvent.create(
                outbox.getReceiverUserId(), outbox.getSenderUserId(), outbox.getType(),
                outbox.getPostId(), outbox.getCommentId(), outbox.getContent(), outbox.getEventId());
        try {
            kafkaTemplate.send(notificationTopic, event.messageKey(), event).get();
            outboxMapper.markSent(outbox.getId(), token);
        } catch (Exception ex) {
            int retries = (outbox.getRetryCount() == null ? 0 : outbox.getRetryCount()) + 1;
            int status;
            LocalDateTime nextRetryAt;
            if (retries >= MAX_RETRIES) {
                status = 3;
                nextRetryAt = LocalDateTime.now();
                log.error("Notification outbox event exhausted retries. id={}, eventId={}", outbox.getId(), outbox.getEventId(), ex);
            } else {
                status = 0;
                nextRetryAt = LocalDateTime.now().plusSeconds(1L << Math.min(retries, 6));
                log.warn("Notification outbox delivery failed. id={}, retry={}", outbox.getId(), retries);
            }
            outboxMapper.markFailedAttempt(outbox.getId(), token, status, retries, nextRetryAt, abbreviate(ex.getMessage()));
        }
    }

    private String abbreviate(String message) {
        if (message == null) return "Kafka send failed";
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
