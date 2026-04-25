package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminPostVO implements Serializable {
    private Long id;
    private String title;
    private String summary;
    private Integer status;
    private Boolean isTop;
    private Boolean isEssence;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Long categoryId;
    private String categoryName;
    private UserVO author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
