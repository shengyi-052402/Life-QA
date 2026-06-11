package com.forum.server.messaging;

import com.forum.server.messaging.event.NotificationEvent;
import com.forum.server.service.NotificationService;
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
public class NotificationEventPublisher {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final NotificationService notificationService;

    @Value("${forum.kafka.topics.notification:forum.notification.events}")
    private String notificationTopic;

    public void publish(Long receiverUserId, Long senderUserId, String type, Long postId, Long commentId, String content) {
        if (receiverUserId == null || senderUserId == null || receiverUserId.equals(senderUserId)) {
            return;
        }

        NotificationEvent event = NotificationEvent.create(receiverUserId, senderUserId, type, postId, commentId, content);
        publishAfterCommit(event);
    }

    private void publishAfterCommit(NotificationEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendAsync(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Only publish after the DB transaction commits, otherwise a rolled-back like/comment could still notify users.
                sendAsync(event);
            }
        });
    }

    private void sendAsync(NotificationEvent event) {
        // Kafka send can wait for broker metadata when Kafka is down; run it outside the request thread.
        CompletableFuture.runAsync(() -> sendToKafka(event));
    }

    private void sendToKafka(NotificationEvent event) {
        try {
            kafkaTemplate.send(notificationTopic, event.messageKey(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Notification Kafka publish failed, fallback to direct DB insert. eventId={}", event.getEventId(), ex);
                            fallbackToDatabase(event);
                        }
                    });
        } catch (Exception ex) {
            log.warn("Notification Kafka publish failed before send, fallback to direct DB insert. eventId={}", event.getEventId(), ex);
            fallbackToDatabase(event);
        }
    }

    private void fallbackToDatabase(NotificationEvent event) {
        // Local development may start without Kafka; this fallback keeps the visible notification feature usable.
        notificationService.createNotification(
                event.getReceiverUserId(),
                event.getSenderUserId(),
                event.getType(),
                event.getPostId(),
                event.getCommentId(),
                event.getContent()
        );
    }
}
