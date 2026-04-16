package com.forum.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/** 发表评论 DTO */
@Data
public class CommentCreateDTO implements Serializable {
    @NotBlank(message = "评论内容不能为空")
    private String content;
    /** 父评论ID, 一级评论不传 */
    private Long parentId;
    /** 回复目标用户ID */
    private Long replyToUserId;
}
