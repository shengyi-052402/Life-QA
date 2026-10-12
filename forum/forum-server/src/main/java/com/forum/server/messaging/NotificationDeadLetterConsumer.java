package com.forum.server.messaging;

import com.forum.server.messaging.event.NotificationEvent;
import com.forum.server.service.NotificationDeliveryFailureService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class NotificationDeadLetterConsumer {
    private final NotificationDeliveryFailureService failureService;

    @KafkaListener(
            topics = "${forum.kafka.topics.notification-dlt:forum.notification.events.DLT}",
            groupId = "${spring.kafka.consumer.group-id:forum-notification-consumer}-dlt"
    )
    public void consume(ConsumerRecord<String, NotificationEvent> record) {
        Header failureHeader = record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE);
        String reason = failureHeader == null ? "Kafka consumer retries exhausted"
                : new String(failureHeader.value(), StandardCharsets.UTF_8);
        failureService.recordConsumerFailure(record.value(), record.topic(), record.partition(), record.offset(), reason);
    }
}
