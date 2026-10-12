package com.forum.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.exception.BaseException;
import com.forum.common.result.PageResult;
import com.forum.pojo.entity.NotificationDeadLetter;
import com.forum.pojo.entity.NotificationOutboxEvent;
import com.forum.pojo.vo.DeliveryFailureVO;
import com.forum.server.mapper.NotificationDeadLetterMapper;
import com.forum.server.mapper.NotificationOutboxEventMapper;
import com.forum.server.messaging.NotificationOutboxDispatcher;
import com.forum.server.messaging.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryFailureService {
    private static final int MAX_AUTO_REPLAYS = 3;

    private final AdminService adminService;
    private final NotificationDeadLetterMapper deadLetterMapper;
    private final NotificationOutboxEventMapper outboxMapper;
    private final NotificationOutboxDispatcher outboxDispatcher;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${forum.kafka.topics.notification:forum.notification.events}")
    private String notificationTopic;

    public PageResult<DeliveryFailureVO> list(String source, Integer page, Integer size) {
        adminService.assertAdmin();
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.min(Math.max(size == null ? 20 : size, 1), 100);
        if ("consumer".equalsIgnoreCase(source)) {
            Page<NotificationDeadLetter> result = new Page<>(safePage, safeSize);
            deadLetterMapper.selectPage(result, new LambdaQueryWrapper<NotificationDeadLetter>()
                    .ne(NotificationDeadLetter::getStatus, 2).orderByDesc(NotificationDeadLetter::getCreatedAt));
            return new PageResult<>(result.getTotal(), result.getRecords().stream()
                    .map(this::toConsumerVO).collect(Collectors.toList()));
        }
        if (!"outbox".equalsIgnoreCase(source)) throw new BaseException("不支持的失败来源");
        Page<NotificationOutboxEvent> result = new Page<>(safePage, safeSize);
        outboxMapper.selectPage(result, new LambdaQueryWrapper<NotificationOutboxEvent>()
                .eq(NotificationOutboxEvent::getStatus, 3).orderByDesc(NotificationOutboxEvent::getCreatedAt));
        return new PageResult<>(result.getTotal(), result.getRecords().stream()
                .map(this::toOutboxVO).collect(Collectors.toList()));
    }

    public void replay(String source, Long id) {
        adminService.assertAdmin();
        if ("consumer".equalsIgnoreCase(source)) {
            NotificationDeadLetter deadLetter = deadLetterMapper.selectById(id);
            if (deadLetter == null) throw new BaseException("消费死信不存在");
            deadLetter.setStatus(0);
            deadLetter.setReplayCount(0);
            deadLetter.setNextRetryAt(LocalDateTime.now());
            deadLetterMapper.updateById(deadLetter);
            replayConsumer(deadLetter);
            return;
        }
        if (!"outbox".equalsIgnoreCase(source)) throw new BaseException("不支持的失败来源");
        if (outboxMapper.resetFailed(id) == 0) throw new BaseException("Outbox 失败记录不存在");
        outboxDispatcher.dispatchPending();
    }

    public void replayBatch(String source, List<Long> ids) {
        adminService.assertAdmin();
        for (Long id : ids) replay(source, id);
    }

    public void recordConsumerFailure(NotificationEvent event, String sourceTopic, Integer sourcePartition,
                                      Long sourceOffset, String failureReason) {
        if (event == null || event.getEventId() == null) return;
        NotificationDeadLetter existing = deadLetterMapper.selectOne(new LambdaQueryWrapper<NotificationDeadLetter>()
                .eq(NotificationDeadLetter::getEventId, event.getEventId()));
        if (existing != null) {
            existing.setFailureReason(abbreviate(failureReason));
            existing.setSourceTopic(sourceTopic);
            existing.setSourcePartition(sourcePartition);
            existing.setSourceOffset(sourceOffset);
            existing.setStatus(value(existing.getReplayCount()) >= MAX_AUTO_REPLAYS ? 3 : 0);
            existing.setNextRetryAt(LocalDateTime.now().plusMinutes(10));
            deadLetterMapper.updateById(existing);
            return;
        }
        try {
            deadLetterMapper.insert(NotificationDeadLetter.builder()
                    .eventId(event.getEventId()).receiverUserId(event.getReceiverUserId())
                    .senderUserId(event.getSenderUserId()).type(event.getType())
                    .postId(event.getPostId()).commentId(event.getCommentId()).content(event.getContent())
                    .sourceTopic(sourceTopic).sourcePartition(sourcePartition).sourceOffset(sourceOffset)
                    .failureReason(abbreviate(failureReason)).status(0).replayCount(0)
                    .nextRetryAt(LocalDateTime.now().plusMinutes(10)).build());
        } catch (DuplicateKeyException ignored) {
            // Another consumer instance recorded the same event concurrently.
        }
    }

    public void resolve(String eventId) {
        if (eventId == null) return;
        NotificationDeadLetter deadLetter = deadLetterMapper.selectOne(new LambdaQueryWrapper<NotificationDeadLetter>()
                .eq(NotificationDeadLetter::getEventId, eventId));
        if (deadLetter == null || Integer.valueOf(2).equals(deadLetter.getStatus())) return;
        deadLetter.setStatus(2);
        deadLetter.setResolvedAt(LocalDateTime.now());
        deadLetterMapper.updateById(deadLetter);
    }

    @Scheduled(fixedDelayString = "${forum.distribution.dead-letter-replay-delay-ms:600000}")
    public void replayDueConsumerFailures() {
        List<NotificationDeadLetter> due = deadLetterMapper.selectList(new LambdaQueryWrapper<NotificationDeadLetter>()
                .in(NotificationDeadLetter::getStatus, List.of(0, 1))
                .lt(NotificationDeadLetter::getReplayCount, MAX_AUTO_REPLAYS)
                .le(NotificationDeadLetter::getNextRetryAt, LocalDateTime.now())
                .orderByAsc(NotificationDeadLetter::getId).last("LIMIT 100"));
        due.forEach(this::replayConsumer);
    }

    private void replayConsumer(NotificationDeadLetter deadLetter) {
        NotificationEvent event = NotificationEvent.create(deadLetter.getReceiverUserId(), deadLetter.getSenderUserId(),
                deadLetter.getType(), deadLetter.getPostId(), deadLetter.getCommentId(), deadLetter.getContent(), deadLetter.getEventId());
        int replayCount = value(deadLetter.getReplayCount()) + 1;
        try {
            kafkaTemplate.send(notificationTopic, event.messageKey(), event).get();
            deadLetter.setStatus(1);
            deadLetter.setReplayCount(replayCount);
            deadLetter.setNextRetryAt(LocalDateTime.now().plusMinutes(10));
            deadLetterMapper.updateById(deadLetter);
        } catch (Exception ex) {
            deadLetter.setReplayCount(replayCount);
            deadLetter.setStatus(replayCount >= MAX_AUTO_REPLAYS ? 3 : 0);
            deadLetter.setFailureReason(abbreviate(ex.getMessage()));
            deadLetter.setNextRetryAt(LocalDateTime.now().plusMinutes(10));
            deadLetterMapper.updateById(deadLetter);
            log.warn("Replay notification dead letter failed, eventId={}", deadLetter.getEventId(), ex);
        }
    }

    private DeliveryFailureVO toOutboxVO(NotificationOutboxEvent event) {
        return DeliveryFailureVO.builder().id(event.getId()).source("outbox").eventId(event.getEventId())
                .status(event.getStatus()).retryCount(event.getRetryCount()).lastError(event.getLastError())
                .nextRetryAt(event.getNextRetryAt()).createdAt(event.getCreatedAt()).build();
    }

    private DeliveryFailureVO toConsumerVO(NotificationDeadLetter event) {
        return DeliveryFailureVO.builder().id(event.getId()).source("consumer").eventId(event.getEventId())
                .status(event.getStatus()).retryCount(event.getReplayCount()).lastError(event.getFailureReason())
                .nextRetryAt(event.getNextRetryAt()).createdAt(event.getCreatedAt()).build();
    }

    private int value(Integer value) { return value == null ? 0 : value; }
    private String abbreviate(String message) {
        if (message == null) return "Kafka consume failed";
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
