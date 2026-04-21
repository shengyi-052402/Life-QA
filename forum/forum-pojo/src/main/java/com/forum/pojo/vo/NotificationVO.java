package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationVO implements Serializable {
    private Long id;
    private String type;
    private Long postId;
    private Long commentId;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private UserVO sender;
}
