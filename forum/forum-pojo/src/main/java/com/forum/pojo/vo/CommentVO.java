package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/** 评论 VO (支持B站风格两级结构) */
@Data
@Builder
public class CommentVO implements Serializable {
    private Long id;
    private String content;
    private UserVO user;
    
    /** 回复目标用户 (仅在二级评论中有值) */
    private UserVO replyToUser;
    
    private Integer likeCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;
    
    /** 一级评论下的回复总数 */
    private Integer replyCount;
    
    /** 子回复列表 (仅在一级评论中带有前N条) */
    private List<CommentVO> replies;
}
