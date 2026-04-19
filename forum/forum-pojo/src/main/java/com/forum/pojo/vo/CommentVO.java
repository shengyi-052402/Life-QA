package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CommentVO {
    private Long id;
    private Long postId;
    private Long parentId;
    
    // 作者信息
    private UserVO author;
    
    // 被回复的用户信息(如果是二级评论互相回复的话)
    private UserVO replyToUser;

    private String content;
    private Integer likeCount;
    private LocalDateTime createdAt;
    
    // 当前登录用户是否点赞
    private Boolean isLiked;

    // 二级评论(子回复)预览列表
    private List<CommentVO> replies;
    
    // 二级评论总数(用于判断是否需要显示“查看全部 x 条回复”)
    private Integer replyCount;
}
