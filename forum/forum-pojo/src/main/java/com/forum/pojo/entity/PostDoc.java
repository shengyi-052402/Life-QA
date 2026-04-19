package com.forum.pojo.entity;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 搜索引擎使用的帖子文档 (本项目已切换至 MySQL 模糊搜索平替，此类已废弃)
 */
@Data
@Builder
public class PostDoc {

    private Long id;
    private String title;
    private String content;
    private String summary;
    private Long userId;
    private String authorName;
    private Long categoryId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
}
