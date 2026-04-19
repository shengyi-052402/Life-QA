package com.forum.pojo.dto;

import lombok.Data;

@Data
public class CommentPageQueryDTO {
    
    // 页码，默认1
    private Integer page = 1;

    // 每页条数
    private Integer size = 10;
    
    // 所属帖子ID (必传)
    private Long postId;

    // 父评论ID (如果是查顶级评论，传0；如果是点击“查看更多回复”，传顶级评论的ID)
    private Long parentId = 0L;
    
    // 排序规则: "latest" 或 "hot"
    private String sort = "latest";
}
