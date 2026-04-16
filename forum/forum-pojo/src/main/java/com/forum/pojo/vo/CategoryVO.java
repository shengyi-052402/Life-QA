package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 分类返回 VO */
@Data
@Builder
public class CategoryVO implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Integer postCount;
    private LocalDateTime createdAt;
}
