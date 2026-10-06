package com.forum.server.messaging;

import com.forum.server.messaging.event.NotificationEvent;
import com.forum.server.service.NotificationService;
import com.forum.server.service.NotificationDeliveryFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final NotificationDeliveryFailureService failureService;

    @KafkaListener(
            topics = "${forum.kafka.topics.notification:forum.notification.events}",
            groupId = "${spring.kafka.consumer.group-id:forum-notification-consumer}",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void consume(NotificationEvent event) {
        if (event == null || event.getEventId() == null) {
            throw new IllegalArgumentException("Notification event and eventId are required");
        }

        try {
            notificationService.createNotificationFromEvent(event.getEventId(), event.getReceiverUserId(), event.getSenderUserId(),
                    event.getType(), event.getPostId(), event.getCommentId(), event.getContent());
            failureService.resolve(event.getEventId());
        } catch (Exception ex) {
            log.warn("Notification event consume failed and will be retried. eventId={}", event.getEventId(), ex);
            throw ex;
        }
    }
}
