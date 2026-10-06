package com.forum.server.messaging;

import com.forum.server.messaging.event.NotificationEvent;
import com.forum.pojo.entity.NotificationOutboxEvent;
import com.forum.server.mapper.NotificationOutboxEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class  NotificationEventPublisher {

    private final NotificationOutboxEventMapper outboxMapper;

    /**
     *
     * @param receiverUserId 帖子作者id
     * @param senderUserId 点赞者id
     * @param type
     * @param postId
     * @param commentId
     * @param content
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(Long receiverUserId, Long senderUserId, String type, Long postId, Long commentId, String content) {
        if (receiverUserId == null || senderUserId == null || receiverUserId.equals(senderUserId)) {
            return;
        }

        outboxMapper.insert(NotificationOutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .receiverUserId(receiverUserId)
                .senderUserId(senderUserId)
                .type(type)
                .postId(postId)
                .commentId(commentId)
                .content(content)
                .status(0)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now())
                .build());
    }
}
