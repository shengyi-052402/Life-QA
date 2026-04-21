package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class CommentLocationVO implements Serializable {
    private Long postId;
    private Long rootCommentId;
    private Long commentId;
    private Integer topPage;
    private Integer replyPage;
}
