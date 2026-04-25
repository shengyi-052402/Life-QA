package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminCommentVO implements Serializable {
    private Long id;
    private String content;
    private Integer status;
    private Long postId;
    private String postTitle;
    private Long parentId;
    private Integer likeCount;
    private UserVO author;
    private UserVO replyToUser;
    private LocalDateTime createdAt;
}
