package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class UserActivityVO implements Serializable {
    private String type;
    private Long targetId;
    private Long postId;
    private String postTitle;
    private String title;
    private String summary;
    private String content;
    private LocalDateTime createdAt;
}
