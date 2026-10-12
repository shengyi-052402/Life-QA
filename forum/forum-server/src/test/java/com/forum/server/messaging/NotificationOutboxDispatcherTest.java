package com.forum.server.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forum.pojo.entity.NotificationOutboxEvent;
import com.forum.server.mapper.NotificationOutboxEventMapper;
import com.forum.server.messaging.event.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationOutboxDispatcherTest {

    @Test
    @SuppressWarnings("unchecked")
    void eventIsNotPublishedWhenAnotherWorkerOwnsTheClaim() {
        NotificationOutboxEventMapper mapper = mock(NotificationOutboxEventMapper.class);
        KafkaTemplate<String, NotificationEvent> kafka = mock(KafkaTemplate.class);
        NotificationOutboxDispatcher dispatcher = new NotificationOutboxDispatcher(mapper, kafka);
        NotificationOutboxEvent event = NotificationOutboxEvent.builder().id(1L).status(0)
                .nextRetryAt(LocalDateTime.now()).build();
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(event));
        when(mapper.claim(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(0);

        dispatcher.dispatchPending();

        verify(kafka, never()).send(anyString(), anyString(), any(NotificationEvent.class));
    }
}
