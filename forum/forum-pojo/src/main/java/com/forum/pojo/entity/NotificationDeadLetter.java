package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification_dead_letter")
public class NotificationDeadLetter {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private Long receiverUserId;
    private Long senderUserId;
    private String type;
    private Long postId;
    private Long commentId;
    private String content;
    private String sourceTopic;
    private Integer sourcePartition;
    private Long sourceOffset;
    private String failureReason;
    /** 0=pending replay, 1=replay published, 2=resolved, 3=exhausted */
    private Integer status;
    private Integer replayCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
