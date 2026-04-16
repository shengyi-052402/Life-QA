package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 收藏实体 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("favorite")
public class Favorite implements Serializable {
    private Long userId;
    private Long postId;
    private LocalDateTime createdAt;
}
