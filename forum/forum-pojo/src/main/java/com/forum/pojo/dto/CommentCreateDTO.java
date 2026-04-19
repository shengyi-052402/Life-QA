package com.forum.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class CommentCreateDTO {
    
    @NotNull(message = "所属帖子ID不能为空")
    private Long postId;

    // 0代表一级评论(直接回复帖子)，其他值代表回复的具体某条评论(盖楼)
    @NotNull(message = "父评论ID不能为空")
    private Long parentId;

    // 被回复的用户ID(仅当是对评论进行回复时，用于在界面显示“回复 @张三”)
    private Long replyToUserId;

    @NotBlank(message = "评论内容不能为空")
    @Length(max = 1000, message = "评论内容不能超过1000字")
    private String content;
}
