package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/** 帖子详情 VO */
@Data
@Builder
public class PostDetailVO implements Serializable {
    private Long id;
    private String title;
    private String content;
    private UserVO author;
    private Long categoryId;
    private String categoryName;
    private List<TagVO> tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Boolean isTop;
    private Boolean isEssence;
    private Boolean isLiked;
    private Boolean isFavorited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
