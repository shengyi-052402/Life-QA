package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 评论点赞实体 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("comment_like")
public class CommentLike implements Serializable {
    private Long userId;
    private Long commentId;
    private LocalDateTime createdAt;
}
