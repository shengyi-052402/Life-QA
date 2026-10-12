package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Transactional outbox for interaction notifications.  The business transaction
 * writes this record; a separate dispatcher owns Kafka delivery and retry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification_outbox_event")
public class NotificationOutboxEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private Long receiverUserId;
    private Long senderUserId;
    private String type;
    private Long postId;
    private Long commentId;
    private String content;
    /** 0=pending, 1=processing, 2=sent, 3=failed */
    private Integer status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lockToken;
    private LocalDateTime lockedUntil;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;
}
