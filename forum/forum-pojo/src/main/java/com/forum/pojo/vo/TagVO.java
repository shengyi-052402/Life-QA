package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 标签返回 VO */
@Data
@Builder
public class TagVO implements Serializable {
    private Long id;
    private String name;
    private Integer postCount;
    private LocalDateTime createdAt;
}
