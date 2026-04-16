package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 帖子点赞实体 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("post_like")
public class PostLike implements Serializable {
    private Long userId;
    private Long postId;
    private LocalDateTime createdAt;
}
