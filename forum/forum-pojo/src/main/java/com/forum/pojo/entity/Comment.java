package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论实体 (支持B站风格楼中楼)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("comment")
public class Comment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    private Long userId;

    private Long postId;

    /** 父评论ID, NULL=一级评论 */
    private Long parentId;

    /** 回复目标用户ID */
    private Long replyToUserId;

    private Integer likeCount;

    /** 0=隐藏, 1=正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
