package com.forum.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 标签返回 VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagVO implements Serializable {
    private Long id;
    private String name;
    private Integer postCount;
    private LocalDateTime createdAt;
}
